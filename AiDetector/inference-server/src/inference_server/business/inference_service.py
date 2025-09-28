from typing import Final

from inference_server.business.model_storage import ModelStorage, ModelDefinition
from inference_server.exception.model import ModelNotExistsException
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)


class InferenceService:
    def __init__(self, model_storage: ModelStorage):
        self.__model_storage: Final[ModelStorage] = model_storage

    def execute_request(
        self, request: ModelInferenceRequestBatch
    ) -> ModelInferenceResultBatch:
        model = self.__get_model_or_throw(request.model_name)

        if not model.is_loaded():
            model.load_model()

        return model.execute(request)

    def __get_model_or_throw(self, model_name: str) -> ModelDefinition:
        model = self.__model_storage.get_model(model_name)

        if model is None:
            raise ModelNotExistsException(
                f"Desired model {model_name} doesn't exist!"
            )

        return model

    def load_model(self, model_name: str) -> bool:
        model = self.__get_model_or_throw(model_name)

        if model.is_loaded():
            return False

        model.load_model()

        return True

    def unload_model(self, model_name: str) -> bool:
        model = self.__get_model_or_throw(model_name)

        if not model.is_loaded():
            return False

        model.unload_model()

        return True

    def is_model_ready(self, model_name: str) -> bool:
        model = self.__get_model_or_throw(model_name)

        return model.is_loaded()