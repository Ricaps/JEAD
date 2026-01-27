import asyncio
import logging
from pathlib import Path
from typing import Final, Optional


from inference_server.business.shutdown_aware import ShutdownAware
from inference_server.configuration.model_config import ModelsConfig, Model
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
    def __init__(self, model_name: str, config: Model, server_config: ServerConfig):
        self.__model_name: str = model_name
        self.__config: Model = config
        self.__model_manager = ModelWorkerManager(config, model_name, server_config)
        self.__logger = logging.getLogger(self.__class__.__name__)

    def is_loaded(self) -> bool:
        return self.__model_manager.is_loaded

    async def load_model(self):
        if self.is_loaded():
            return

        await self.__model_manager.start_connection()
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
        return self.__model_name


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

    async def load_models(self):
        self._logger.info(
            "Looking for models in %s config", self._server_config.model_config_path
        )

        config = ModelsConfig.from_yaml(Path(self._server_config.model_config_path))

        async with self.__model_holder_lock:
            for name, model in config.models.items():
                self._logger.info("Found model definition: %s", name)
                definition = ModelDefinition(name, model, self._server_config)
                self.__model_holder[name] = definition

        if len(self.__model_holder.keys()) == 0:
            self._logger.info("No model defined!")

    async def on_shutdown(self) -> None:
        async with self.__model_holder_lock:
            for model_name, model in self.__model_holder.items():
                await model.unload_model()
