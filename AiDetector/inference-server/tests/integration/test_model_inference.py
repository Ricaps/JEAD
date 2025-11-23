from grpc import StatusCode
from grpc.aio import AioRpcError

from inference_server.proto.inference_pb2 import (
    InferenceRequest,
    InferenceResponse,
    ModelReadyResponse,
    ModelNameRequest,
)
from tests.util.grpc_test import AsyncGrpcTestCase


class TestModelInferenceIntegration(AsyncGrpcTestCase):
    INFERENCE_CONTENT = [
        {"id": "random-id-1", "content": "Sentence 1"},
        {"id": "random-id-2", "content": "Sentence 2"},
    ]

    async def testModelInference_nonExistingModel_throwsError(self):
        model_name = "random-model"
        with self.assertRaises(AioRpcError) as error:
            await self.get_stub().ModelInference(
                self.create_inference_request(model_name)
            )

        self.assertEqual(error.exception.code(), StatusCode.FAILED_PRECONDITION)
        self.assertEqual(
            error.exception.details(), f"Desired model {model_name} doesn't exist!"
        )

    async def testModelInference_existingModel_success(self):
        response: InferenceResponse = await self.get_stub().ModelInference(
            self.create_inference_request(self.EXISTING_MODEL_NAME)
        )
        self.assertEqual(len(response.contents), 2)
        content_0, content_1 = response.contents
        self.assertEqual(content_0.id, self.INFERENCE_CONTENT[0]["id"])
        self.assertEqual(content_1.id, self.INFERENCE_CONTENT[1]["id"])

    async def testModelInference_automaticLoading(self):
        model_ready_res: ModelReadyResponse = await self.get_stub().ModelReady(
            ModelNameRequest(model_name=self.EXISTING_MODEL_NAME)
        )
        self.assertFalse(model_ready_res.ready)

        await self.get_stub().ModelInference(
            self.create_inference_request(self.EXISTING_MODEL_NAME)
        )

        model_ready_res: ModelReadyResponse = await self.get_stub().ModelReady(
            ModelNameRequest(model_name=self.EXISTING_MODEL_NAME)
        )
        self.assertTrue(model_ready_res.ready)

    @staticmethod
    def create_inference_request(model_name: str):
        return InferenceRequest(
            model_name=model_name,
            contents=TestModelInferenceIntegration.INFERENCE_CONTENT,
        )
