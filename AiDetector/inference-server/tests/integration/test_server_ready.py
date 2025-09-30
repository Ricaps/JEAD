from inference_server.proto.inference_pb2 import ServerReadyRequest, ServerReadyResponse
from util.grpc_test import AsyncGrpcTestCase


class TestServerReadyIntegration(AsyncGrpcTestCase):
    async def testServerResponded(self):
        response: ServerReadyResponse = await self.get_stub().ServerReady(
            ServerReadyRequest()
        )
        self.assertTrue(response.ready)
