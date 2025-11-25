from grpc import StatusCode
from grpc.aio import AioRpcError

from inference_server.proto.inference_pb2 import ModelNameRequest, ModelReadyResponse
from tests.util.grpc_test import AsyncGrpcTestCase


class TestModelReadyIntegration(AsyncGrpcTestCase):
    async def testModelReady_existingModelNotLoaded_returnsFalse(self):
        response: ModelReadyResponse = await self.get_stub().ModelReady(
            ModelNameRequest(model_name=self.EXISTING_MODEL_NAME)
        )
        self.assertFalse(response.ready)

    async def testModelReady_existingModelLoaded_returnsTrue(self):
        await self.load_model()
        response = await self.get_stub().ModelReady(
            ModelNameRequest(model_name=self.EXISTING_MODEL_NAME)
        )
        self.assertTrue(response.ready)

        await self.unload_model()

        response = await self.get_stub().ModelReady(
            ModelNameRequest(model_name=self.EXISTING_MODEL_NAME)
        )
        self.assertFalse(response.ready)

    async def testModelReady_nonExistingModel_throwsError(self):
        model_name = "random-model"
        with self.assertRaises(AioRpcError) as error:
            await self.get_stub().ModelReady(ModelNameRequest(model_name=model_name))

        self.assertEqual(error.exception.code(), StatusCode.FAILED_PRECONDITION)
        self.assertEqual(
            error.exception.details(), f"Desired model {model_name} doesn't exist!"
        )

    async def load_model(self):
        await self.get_stub().LoadModel(
            ModelNameRequest(model_name=TestModelReadyIntegration.EXISTING_MODEL_NAME)
        )

    async def unload_model(self):
        await self.get_stub().UnloadModel(
            ModelNameRequest(model_name=TestModelReadyIntegration.EXISTING_MODEL_NAME)
        )
