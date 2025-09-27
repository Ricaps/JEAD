from typing import Final
from inference_server.proto.inference_pb2_grpc import InferenceServiceServicer
from inference_server.proto.inference_pb2 import ServerReadyRequest, ServerReadyResponse
from inference_server.business.inference_service import InferenceService


class InferenceServicerPort(InferenceServiceServicer):
    def __init__(self, inference_service: InferenceService):
        super().__init__()
        self._inference_service: Final[InferenceService] = inference_service

    def ServerReady(self, request: ServerReadyRequest, context):
        return ServerReadyResponse(ready=True)
