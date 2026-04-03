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
    def __init__(self, model_path: AsyncPath, server_config: ServerConfig):
        self.__model_path: Final[AsyncPath] = model_path
        self.__model_manager = ModelWorkerManager(model_path, server_config)
        self.__logger = logging.getLogger(
            self.__class__.__name__ + f"#{self.__model_path.name}"
        )
        self.__loading_lock = asyncio.Lock()

    def is_loaded(self) -> bool:
        return self.__model_manager.is_loaded

    async def load_model(self):
        async with self.__loading_lock:
            if self.is_loaded():
                return

            await self.__model_manager.start_process()
            await self.__model_manager.send(
                Message(command=WorkerCommand.LOAD, data={})
            )
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
    PIP_SCRIPT_URL = "https://bootstrap.pypa.io/get-pip.py"
    PIP_SCRIPT_NAME = "get-pip.py"

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

        return ModelDefinition(model_folder, self._server_config)

    async def load_models(self):
        models_root = AsyncPath(self._server_config.models_root)
        self._logger.info("Looking for models in %s directory", models_root)

        if not await models_root.exists():
            self._logger.error(
                f"Models root directory '{models_root}' doesn't exist! No model will be loaded."
            )
            return

        result = await self.ensure_venv(models_root)
        if not result:
            return

        result = await self._install_requirements(models_root)
        if not result:
            return

        async for folder in models_root.iterdir():
            if folder.name == self._server_config.models_venv_dir_name:
                continue

            if await folder.is_file():
                continue

            worker_path = folder / ModelWorkerManager.WORKER_FILE
            if not await worker_path.exists():
                self._logger.debug(
                    f"Model folder '{folder}' doesn't include {worker_path}!"
                )
                continue

            model = await self.__load_model(folder)
            if model is None:
                continue

            self._logger.info("Found model %s in %s", model.name, str(folder))
            async with self.__model_holder_lock:
                self.__model_holder[model.name] = model

        if len(self.__model_holder.keys()) == 0:
            self._logger.info("No model found!")

    async def on_shutdown(self) -> None:
        async with self.__model_holder_lock:
            for model_name, model in self.__model_holder.items():
                await model.unload_model()

    async def ensure_venv(self, model_root: AsyncPath) -> bool:
        """
        Ensure that a virtual environment exists for the model located at model_root.
        If it doesn't exist, create it and install pip.
        Pip is installed using the get-pip.py script fetched from the official source.

        Args:
            model_root: path to the model root directory.

        Returns: True if the virtual environment exists or was created successfully, False otherwise.

        """
        venv_path, python_path, pip_path, req_path = self.get_paths(model_root)

        if await venv_path.exists():
            if await python_path.exists() and await pip_path.exists():
                return True

            self._logger.warning(
                f"Model root '{model_root}' already include .venv folder, but it's not correctly installed!"
            )
            # TODO: remove incorrect venv

        self._logger.info(
            f"Creating virtual environment for models root at '{model_root}'"
        )
        try:
            await asyncio.to_thread(venv.create, (str(venv_path)), with_pip=False)
        except Exception as e:
            self._logger.error(
                f"Failed to create virtual environment for model at '{model_root}': {e}"
            )
            return False

        try:
            get_pip_url = ModelStorage.PIP_SCRIPT_URL
            get_pip_path = venv_path / ModelStorage.PIP_SCRIPT_NAME

            self._logger.info(
                f"Downloading get-pip.py for models root at '{model_root}'"
            )
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
                    f"Failed to install pip for model root at '{model_root}': {stderr.decode()}"
                )
                return False

            await get_pip_path.unlink()

        except Exception as e:
            self._logger.error(
                f"Failed to install pip for model root at '{model_root}': {e}"
            )
            return False

        return True

    async def _install_requirements(self, model_root: AsyncPath) -> bool:
        """
        Install requirements for the model located at model_root.
        It looks for the standard requirements.txt file in the model_root directory.
        Args:
            model_root: path to the model root directory.

        Returns: True if installation was successful, False otherwise.

        """
        _, _, pip_path, req_path = self.get_paths(model_root)

        self._logger.info(
            f"Installing requirements for model root at '{model_root}' using '{req_path}'..."
        )

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
                f"Failed to install requirements for model at '{model_root}'. Exit code: {process.returncode}"
            )
            return False

        self._logger.info(
            f"Successfully installed requirements for model root at '{model_root}'"
        )

        return True

    def get_paths(self, model_root: AsyncPath):
        venv_path = model_root / self._server_config.models_venv_dir_name
        python_path = venv_path / "bin" / "python3"
        pip_path = venv_path / "bin" / "pip"

        requirements_file_name = (
            ModelWorkerManager.REQUIREMENTS_GPU_FILE
            if self._server_config.use_gpu
            else ModelWorkerManager.REQUIREMENTS_FILE
        )
        req_path = model_root / requirements_file_name

        return venv_path, python_path, pip_path, req_path
