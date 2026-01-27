import asyncio
import logging
import urllib.request
import venv
from typing import Final, Optional
from aiopath import AsyncPath

from inference_server.business.shutdown_aware import ShutdownAware
from inference_server.exception.model import ModelInferenceException
from inference_server.ml_models.inference_model import (
    InferenceModelExecutable,
)
from inference_server.configuration.config import ServerConfig
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)
from inference_server.module_worker.model_worker_manager import (
    ModelWorkerManager,
    Message,
    WorkerCommand,
)


class ModelDefinition(InferenceModelExecutable):
    def __init__(self, model_path: AsyncPath):
        self.__model_path: Final[AsyncPath] = model_path
        self.__model_manager = ModelWorkerManager(model_path)
        self.__logger = logging.getLogger(self.__class__.__name__)

    def is_loaded(self) -> bool:
        return self.__model_manager.is_loaded

    async def load_model(self):
        if self.is_loaded():
            return

        await self.__model_manager.start_process()
        await self.__model_manager.send(Message(command=WorkerCommand.LOAD, data={}))
        self.__logger.info("Model %s loaded", self.name)

    async def unload_model(self):
        await self.__model_manager.shutdown()
        self.__logger.info("Model %s unloaded", self.name)

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        if not self.is_loaded():
            await self.load_model()

        response = await self.__model_manager.send(
            Message(command=WorkerCommand.INFERENCE, data=data)
        )
        if not response.success:
            raise ModelInferenceException(response.error)

        return response.data

    @property
    def name(self) -> str:
        return self.__model_path.name


class ModelStorage(ShutdownAware):
    def __init__(
        self,
        server_config: ServerConfig,
    ):
        self._server_config: Final[ServerConfig] = server_config
        self.__model_holder: dict[str, ModelDefinition] = dict()
        self.__model_holder_lock = asyncio.Lock()
        self._logger = logging.getLogger(self.__class__.__name__)

    def get_model(self, model_name: str) -> Optional[ModelDefinition]:
        return self.__model_holder.get(model_name)

    async def __load_model(self, model_folder: AsyncPath) -> Optional[ModelDefinition]:
        for file_name in (ModelWorkerManager.WORKER_FILE,):
            file_path = model_folder / file_name
            exists = await file_path.exists()

            if not exists:
                self._logger.warning(
                    f"Model folder '{model_folder}' doesn't include needed file '{file_name}'!"
                )
                return None

        return ModelDefinition(model_folder)

    async def load_models(self):
        models_root = AsyncPath(self._server_config.models_root)
        self._logger.info("Looking for models in %s directory", models_root)

        async for file in models_root.iterdir():
            if await file.is_file():
                continue

            model = await self.__load_model(file)
            if model is None:
                continue

            _, _, _, req_path, worker_path = self.get_paths(file)

            for checked_file in (worker_path, req_path):
                if not await checked_file.exists():
                    self._logger.warning(
                        f"Model folder '{file}' doesn't include {checked_file}!"
                    )
                    continue

            result = await self.ensure_venv(file)
            if not result:
                continue

            result = await self.install_requirements(file)
            if not result:
                continue

            self._logger.info("Found model %s in %s", model.name, str(file))
            async with self.__model_holder_lock:
                self.__model_holder[model.name] = model

        if len(self.__model_holder.keys()) == 0:
            self._logger.info("No model found!")

    async def on_shutdown(self) -> None:
        async with self.__model_holder_lock:
            for model_name, model in self.__model_holder.items():
                await model.unload_model()

    async def ensure_venv(self, file: AsyncPath) -> bool:
        venv_path, python_path, pip_path, req_path, _ = self.get_paths(file)

        if await venv_path.exists():
            if await python_path.exists() and await pip_path.exists():
                return True

            self._logger.warning(
                f"Model folder '{file}' already include .venv folder, but it's not correctly installed!"
            )

        try:
            await asyncio.to_thread(venv.create, (str(venv_path)), with_pip=False)
        except Exception as e:
            self._logger.error(
                f"Failed to create virtual environment for model at '{file}': {e}"
            )
            return False

        try:
            get_pip_url = "https://bootstrap.pypa.io/get-pip.py"
            get_pip_path = venv_path / "get-pip.py"

            self._logger.info(f"Downloading get-pip.py for model at '{file}'")
            await asyncio.to_thread(
                urllib.request.urlretrieve, get_pip_url, str(get_pip_path)
            )

            process = await asyncio.create_subprocess_exec(
                str(python_path),
                str(get_pip_path),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            stdout, stderr = await process.communicate()

            if process.returncode != 0:
                self._logger.error(
                    f"Failed to install pip for model at '{file}': {stderr.decode()}"
                )
                return False

            await get_pip_path.unlink()

        except Exception as e:
            self._logger.error(f"Failed to install pip for model at '{file}': {e}")
            return False

        return True

    async def install_requirements(self, file_path: AsyncPath) -> bool:
        _, _, pip_path, req_path, _ = self.get_paths(file_path)

        self._logger.info(f"Installing requirements for model at '{file_path}'...")

        process = await asyncio.create_subprocess_exec(
            str(pip_path),
            "install",
            "-r",
            str(req_path),
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )

        async for line in process.stdout:
            line_str = line.decode().rstrip()
            if line_str:
                self._logger.info(f"[pip] {line_str}")

        await process.wait()

        if process.returncode != 0:
            self._logger.error(
                f"Failed to install requirements for model at '{file_path}'. Exit code: {process.returncode}"
            )
            return False

        self._logger.info(
            f"Successfully installed requirements for model at '{file_path}'"
        )

        return True

    @staticmethod
    def get_paths(model_path: AsyncPath):
        venv_path = model_path / ModelWorkerManager.VENV_FOLDER
        python_path = venv_path / "bin" / "python3"
        pip_path = venv_path / "bin" / "pip"
        req_path = model_path / ModelWorkerManager.REQUIREMENTS_FILE
        worker_path = model_path / ModelWorkerManager.WORKER_FILE

        return venv_path, python_path, pip_path, req_path, worker_path
