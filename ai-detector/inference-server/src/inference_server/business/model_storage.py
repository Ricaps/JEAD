import asyncio
import logging
from typing import Final, Optional
from aiopath import AsyncPath

from inference_server.business.shutdown_aware import ShutdownAware
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
    Command,
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
        await self.__model_manager.send(Command(command="load", data={}))
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
            Command(command="inference", data=data)
        )
        if not response.success:
            return None

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

            self._logger.info("Found model %s in %s", model.name, str(file))
            async with self.__model_holder_lock:
                self.__model_holder[model.name] = model

        if len(self.__model_holder.keys()) == 0:
            self._logger.info("No model found!")

    async def on_shutdown(self) -> None:
        async with self.__model_holder_lock:
            for model_name, model in self.__model_holder.items():
                await model.unload_model()
