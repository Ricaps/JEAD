import asyncio
import logging
import shutil
import urllib.request
import venv
from contextlib import suppress
from pathlib import Path
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
from inference_server.util.path_resolver import PathResolver


class ModelDefinition(InferenceModelExecutable):
    """Runtime wrapper over a single model worker process."""

    def __init__(self, model_path: AsyncPath, server_config: ServerConfig):
        self.__model_path: Final[AsyncPath] = model_path
        self.__model_manager = ModelWorkerManager(model_path, server_config)
        self.__logger = logging.getLogger(
            self.__class__.__name__ + f"#{self.__model_path.name}"
        )
        self.__loading_lock = asyncio.Lock()

    def is_loaded(self) -> bool:
        """Return whether the model worker is running and ready for inference."""
        return self.__model_manager.is_loaded

    async def load_model(self):
        """Start worker process and send load command once for this model."""
        async with self.__loading_lock:
            if self.is_loaded():
                return

            await self.__model_manager.start_process()
            await self.__model_manager.send_load_command()
            self.__logger.info("Model %s loaded", self.name)

    async def unload_model(self):
        """Stop the model worker process and release resources."""
        await self.__model_manager.shutdown()
        self.__logger.info("Model %s unloaded", self.name)

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        """Execute model inference, loading the worker on first use."""
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
        """Model name derived from the model directory name."""
        return self.__model_path.name


class ModelStorage(ShutdownAware):
    """Discover models on startup and provide access to model definitions."""

    PIP_SCRIPT_URL = "https://bootstrap.pypa.io/get-pip.py"

    def __init__(
        self,
        server_config: ServerConfig,
    ):
        self._server_config: Final[ServerConfig] = server_config
        self.__model_holder: dict[str, ModelDefinition] = dict()
        self.__model_holder_lock = asyncio.Lock()
        self._path_resolver = PathResolver(server_config)
        self._logger = logging.getLogger(self.__class__.__name__)

    def get_model(self, model_name: str) -> Optional[ModelDefinition]:
        """Return a model definition by name, if it was discovered."""
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
        """
        Load available models from the configured models root.

        Model discovery is startup-only. If models are already loaded, this call is
        a no-op to avoid rediscovery and registry replacement.
        """
        async with self.__model_holder_lock:
            if self.__model_holder:
                self._logger.info("Models are already loaded; skipping rediscovery.")
                return

        models_root = AsyncPath(self._server_config.models_root)
        self._logger.info("Looking for models in %s directory", models_root)

        if not await models_root.exists():
            self._logger.error(
                "Models root directory '%s' doesn't exist! No model will be loaded.",
                models_root,
            )
            return

        if not await models_root.is_dir():
            self._logger.error(
                "Models root path '%s' is not a directory! No model will be loaded.",
                models_root,
            )
            return

        if not await self.ensure_venv(models_root):
            return

        if not await self._install_requirements(models_root):
            return

        discovered_models: dict[str, ModelDefinition] = {}
        async for folder in models_root.iterdir():
            if folder.name == self._server_config.models_venv_dir_name:
                continue

            if await folder.is_file():
                continue

            worker_path = folder / ModelWorkerManager.WORKER_FILE
            if not await worker_path.exists():
                self._logger.debug(
                    "Model folder '%s' doesn't include %s!",
                    folder,
                    worker_path,
                )
                continue

            model = await self.__load_model(folder)
            if model is None:
                continue

            self._logger.info("Found model %s in %s", model.name, str(folder))
            discovered_models[model.name] = model

        async with self.__model_holder_lock:
            self.__model_holder = discovered_models

        if len(discovered_models.keys()) == 0:
            self._logger.info("No model found!")

    async def on_shutdown(self) -> None:
        """Unload all discovered models, continuing even if one unload fails."""
        async with self.__model_holder_lock:
            models = list(self.__model_holder.values())

        for model in models:
            try:
                await model.unload_model()
            except Exception:
                self._logger.exception(
                    "Failed to unload model %s during shutdown.", model.name
                )

    async def ensure_venv(self, model_root: AsyncPath) -> bool:
        """
        Ensure that a virtual environment exists for the model located at model_root.
        If it doesn't exist, create it and install pip.
        Pip is installed using the get-pip.py script fetched from the official source.

        Args:
            model_root: path to the model root directory.

        Returns: True if the virtual environment exists or was created successfully, False otherwise.

        """
        venv_path, python_path, pip_path = self._path_resolver.get_python_paths()

        if await venv_path.exists():
            if await python_path.exists() and await pip_path.exists():
                return True

            self._logger.warning(
                f"Model root '{model_root}' already include {venv_path.name} folder, but it's not correctly installed!"
            )
            try:
                await asyncio.to_thread(shutil.rmtree, str(venv_path))
            except Exception as e:
                self._logger.error(
                    "Failed to remove incorrect venv at '%s': %s", venv_path, e
                )
                return False

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

        get_pip_url = ModelStorage.PIP_SCRIPT_URL
        get_pip_path = self._path_resolver.get_pip_script_path()
        try:
            self._logger.info(
                f"Downloading get-pip.py for models root at '{model_root}'"
            )
            await self._download_file(get_pip_url, get_pip_path)

            if not await python_path.exists():
                self._logger.error(f"Python executable not found in {venv_path}!")
                return False

            process = await asyncio.create_subprocess_exec(
                str(python_path),
                str(get_pip_path),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            try:
                _, stderr = await asyncio.wait_for(
                    process.communicate(),
                    timeout=self._server_config.model_command_timeout,
                )
            except TimeoutError:
                process.kill()
                await process.wait()
                self._logger.error(
                    "Timed out while installing pip for model root at '%s'.",
                    model_root,
                )
                return False

            if process.returncode != 0:
                self._logger.error(
                    "Failed to install pip for model root at '%s': %s",
                    model_root,
                    stderr.decode(errors="replace"),
                )
                return False

        except Exception as e:
            self._logger.error(
                "Failed to install pip for model root at '%s': %s", model_root, e
            )
            return False
        finally:
            if await get_pip_path.exists():
                with suppress(Exception):
                    await get_pip_path.unlink()

        return True

    async def _download_file(self, url: str, target_path: AsyncPath):
        def _download():
            with urllib.request.urlopen(url, timeout=30) as response:
                data = response.read()

            Path(target_path).write_bytes(data)

        await asyncio.to_thread(_download)

    async def _install_requirements(self, model_root: AsyncPath) -> bool:
        """
        Install requirements for the model located at model_root.
        It looks for the standard requirements.txt file in the model_root directory.
        Args:
            model_root: path to the model root directory.

        Returns: True if installation was successful, False otherwise.

        """
        _, _, pip_path = self._path_resolver.get_python_paths()
        req_path = self._path_resolver.get_requirements_path()
        if not await pip_path.exists():
            self._logger.error(
                "pip executable not found in expected path '%s'", pip_path
            )
            return False

        if not await req_path.exists():
            self._logger.error("Requirements file '%s' does not exist.", req_path)
            return False

        self._logger.info(
            f"Installing requirements for model root at '{model_root}' using '{req_path}'..."
        )

        try:
            process = await asyncio.create_subprocess_exec(
                str(pip_path),
                "install",
                "-r",
                str(req_path),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.STDOUT,
            )
        except Exception as e:
            self._logger.error(
                "Failed to start pip install process for model root at '%s': %s",
                model_root,
                e,
            )
            return False

        async for line in process.stdout:
            line_str = line.decode().rstrip()
            if line_str:
                self._logger.info(f"[pip] {line_str}")

        return_code = await process.wait()

        if process.returncode != 0:
            self._logger.error(
                "Failed to install requirements for model at '%s'. Exit code: %s",
                model_root,
                return_code,
            )
            return False

        self._logger.info(
            "Successfully installed requirements for model root at '%s'", model_root
        )

        return True
