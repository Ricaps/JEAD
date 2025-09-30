import asyncio
import logging
import gc
import weakref
from _weakref import ReferenceType
from typing import Final, Optional
from aiopath import AsyncPath

from inference_server.ml_models.inference_model import (
    InferenceModel,
    InferenceModelExecutable,
)
from inference_server.configuration.config import ServerConfig
from inference_server.model.inference_model import ModelInferenceRequestBatch


class ModelDefinition(InferenceModelExecutable):
    def __init__(self, model_path: AsyncPath, model_ref_type: type[InferenceModel]):
        self.__model_path: Final[AsyncPath] = model_path
        self.__model_ref_type: Final[type[InferenceModel]] = model_ref_type
        self.__model_ref: Optional[InferenceModel] = None
        self.__logger = logging.getLogger(self.__class__.__name__)

    def is_loaded(self) -> bool:
        return self.__model_ref is not None

    async def load_model(self):
        if self.__model_ref is not None:
            return

        self.__model_ref = self.__model_ref_type(self.__model_path)
        await self.__model_ref.on_load()
        self.__logger.info("Model %s loaded", self.name)

    async def unload_model(self):
        if self.__model_ref is None:
            return

        await self.__model_ref.on_unload()
        self.__model_ref = None
        gc.collect()
        self.__logger.info("Model %s unloaded", self.name)

    async def execute(self, data: ModelInferenceRequestBatch):
        if self.__model_ref is None:
            return None

        return await self.__model_ref.execute(data)

    @property
    def _model_reference(self) -> Optional[ReferenceType[InferenceModel]]:
        if self.__model_ref is None:
            return None

        return weakref.ref(self.__model_ref)

    @property
    def name(self) -> str:
        return self.__model_path.name


class ModelStorage:
    def __init__(
        self,
        server_config: ServerConfig,
        model_type_registry: dict[str, type[InferenceModel]],
    ):
        self._server_config: Final[ServerConfig] = server_config
        self.__model_holder: dict[str, ModelDefinition] = dict()
        self.__model_holder_lock = asyncio.Lock()
        self._model_type_registry = model_type_registry
        self._logger = logging.getLogger(self.__class__.__name__)

    def get_model(self, model_name: str) -> Optional[ModelDefinition]:
        return self.__model_holder.get(model_name)

    def __load_model(self, model_folder: AsyncPath) -> Optional[ModelDefinition]:
        model_name = model_folder.name
        model_type = self._model_type_registry.get(model_name)

        if model_type is None:
            return None

        return ModelDefinition(model_folder, model_type)

    async def load_models(self):
        models_root = AsyncPath(self._server_config.models_root)
        self._logger.info("Looking for models in %s directory", models_root)

        async for file in models_root.iterdir():
            if await file.is_file():
                continue

            model = self.__load_model(file)
            if model is None:
                return

            self._logger.info("Found model %s in %s", model.name, str(file))
            async with self.__model_holder_lock:
                self.__model_holder[model.name] = model

        if len(self.__model_holder.keys()) == 0:
            self._logger.info("No model found!")
