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
    ModelReadyResponse,
    ModelNameRequest,
    SuccessResponse,
)
from inference_server.business.inference_service import InferenceService


class ServerReadyModel(BaseModel):
    ready: bool


class InferenceServicerPort(InferenceServiceServicer):
    def __init__(self, inference_service: InferenceService):
        super().__init__()
        self._inference_service: Final[InferenceService] = inference_service

    async def ServerReady(self, request: ServerReadyRequest, context) -> ServerReadyResponse:
        return ServerReadyResponse(ready=True)

    async def ModelReady(self, request: ModelNameRequest, context) -> ModelReadyResponse:
        return ModelReadyResponse(
            ready=self._inference_service.is_model_ready(request.model_name)
        )

    async def LoadModel(self, request: ModelNameRequest, context) -> SuccessResponse:
        loaded = await self._inference_service.load_model(request.model_name)

        return SuccessResponse(success=loaded)

    async def UnloadModel(self, request: ModelNameRequest, context) -> SuccessResponse:
        unloaded = await self._inference_service.unload_model(request.model_name)

        return SuccessResponse(success=unloaded)

    async def ModelInference(self, request: InferenceRequest, context):
        result = await self._inference_service.execute_request(
            grpc_to_model(request, ModelInferenceRequestBatch)
        )
        return model_to_grpc(result, InferenceResponse)
