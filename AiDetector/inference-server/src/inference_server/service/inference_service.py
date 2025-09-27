from inference_server.proto.inference_pb2_grpc import InferenceServiceServicer
from inference_server.proto.inference_pb2 import ServerReadyRequest, ServerReadyResponse


class InferenceServiceImpl(InferenceServiceServicer):
    def ServerReady(self, request: ServerReadyRequest, context):
        return ServerReadyResponse(ready=True)
