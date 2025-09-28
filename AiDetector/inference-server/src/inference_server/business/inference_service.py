from typing import Final

from inference_server.business.model_storage import ModelStorage
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
        model = self.__model_storage.get_model(request.model_name)

        if model is None:
            raise ModelNotExistsException(
                f"Desired model {request.model_name} doesn't exist!"
            )

        if not model.is_loaded():
            model.load_model()

        return model.execute(request)
