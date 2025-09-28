from typing import Final

from pydantic import BaseModel


from inference_server.model.inference_model import ModelInferenceRequestBatch
from inference_server.model.mapping.inference_model import grpc_to_model, model_to_grpc
from inference_server.proto.inference_pb2_grpc import InferenceServiceServicer
from inference_server.proto.inference_pb2 import (
    ServerReadyRequest,
    ServerReadyResponse,
    InferenceRequest,
    InferenceResponse,
)
from inference_server.business.inference_service import InferenceService


class ServerReadyModel(BaseModel):
    ready: bool


class InferenceServicerPort(InferenceServiceServicer):
    def __init__(self, inference_service: InferenceService):
        super().__init__()
        self._inference_service: Final[InferenceService] = inference_service

    def ServerReady(self, request: ServerReadyRequest, context):
        return ServerReadyResponse(ready=True)

    def ModelInference(self, request: InferenceRequest, context) -> InferenceResponse:
        result = self._inference_service.execute_request(
            grpc_to_model(request, ModelInferenceRequestBatch)
        )
        return model_to_grpc(result, InferenceResponse)
