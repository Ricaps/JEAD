import logging
import gc
from typing import Final, Optional
from pathlib import Path

from inference_server.ml_models.inference_model import (
    InferenceModel,
    InferenceModelExecutable,
)
from inference_server.configuration.config import ServerConfig
from inference_server.ml_models import model_type_registry
from inference_server.model.inference_model import ModelInferenceRequestBatch


class ModelDefinition(InferenceModelExecutable):
    def __init__(self, model_path: Path, model_ref_type: type[InferenceModel]):
        self.__model_path: Final[Path] = model_path
        self.__model_ref_type: Final[type[InferenceModel]] = model_ref_type
        self.__model_ref: Optional[InferenceModel] = None
        self.__logger = logging.getLogger(self.__class__.__name__)

    def is_loaded(self) -> bool:
        return self.__model_ref is not None

    def load_model(self):
        self.__model_ref = self.__model_ref_type(self.__model_path)
        self.__model_ref.on_load()
        self.__logger.info("Model %s loaded", self.name)

    def unload_model(self):
        if self.__model_ref is None:
            return

        self.__model_ref.on_unload()
        self.__model_ref = None
        gc.collect()
        self.__logger.info("Model %s unloaded", self.name)

    def execute(self, data: ModelInferenceRequestBatch):
        if self.__model_ref is None:
            return None

        return self.__model_ref.execute(data)

    @property
    def name(self) -> str:
        return self.__model_path.name


class ModelStorage:
    def __init__(self, server_config: ServerConfig):
        self.server_config: Final[ServerConfig] = server_config
        self.logger = logging.getLogger(self.__class__.__name__)
        self.__model_holder: dict[str, ModelDefinition] = dict()

    def get_model(self, model_name: str) -> Optional[ModelDefinition]:
        return self.__model_holder.get(model_name)

    @staticmethod
    def __load_model(model_folder: Path) -> Optional[ModelDefinition]:
        model_name = model_folder.name
        model_type = model_type_registry.get(model_name)

        if model_type is None:
            return None

        return ModelDefinition(model_folder, model_type)

    def load_models(self):
        models_root = Path(self.server_config.models_root)
        self.logger.info("Looking for models in %s directory", models_root)

        for file in models_root.iterdir():
            if file.is_file():
                continue

            model = self.__load_model(file)
            if model is None:
                return

            self.logger.info("Found model %s in %s", model.name, str(file))
            self.__model_holder[model.name] = model

        if len(self.__model_holder.keys()) == 0:
            self.logger.info("No model found!")
