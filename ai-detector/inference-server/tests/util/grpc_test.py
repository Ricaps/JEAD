import socket
import subprocess
import sys
from typing import Optional
from unittest.async_case import IsolatedAsyncioTestCase

import grpc.aio as grpc
from grpc.aio import Server

from inference_server.business.inference_service import InferenceService
from inference_server.business.model_storage import ModelStorage
from inference_server.configuration.config import ServerConfig
from inference_server.grpc_service.inference_port import InferenceServicerPort
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
    LabelEvaluation,
    ModelInferenceResult,
)
from inference_server.proto import inference_pb2_grpc
from inference_server.server.grpc_server import create_server
from inference_server.proto.inference_pb2_grpc import InferenceServiceStub


class DummyModel(InferenceModel):
    async def on_load(self):
        pass

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        label_1 = LabelEvaluation(label="label-1", score=0.0005)
        label_2 = LabelEvaluation(label="label-2", score=0.99)

        mirrored_result = list(
            map(
                lambda element: ModelInferenceResult(
                    id=element.id, label_evaluation=[label_1, label_2]
                ),
                data.contents,
            )
        )

        return ModelInferenceResultBatch(contents=mirrored_result)

    async def on_unload(self):
        pass


class AsyncGrpcTestCase(IsolatedAsyncioTestCase):
    ADDRESS = "localhost:55555"
    EXISTING_MODEL_NAME = "existing-model"
    SOCKET_WORKER_PATH = "models/socket_worker.py"
    EXISTING_MODEL_WORKER = "tests/resources/existing_model/worker.py"

    @staticmethod
    def _get_port():
        sock = socket.socket()
        sock.bind(("", 0))
        port = sock.getsockname()[1]
        sock.close()

        return port

    async def asyncSetUp(self):
        test_config = ServerConfig(
            address="0.0.0.0", port="0", model_config_path="tests/resources/models.yaml"
        )
        self.server = await create_server()
        await self._add_services(self.server, test_config)
        self.server.add_insecure_port(self.ADDRESS)
        await self.server.start()

        self.channel = grpc.insecure_channel(self.ADDRESS)
        self.test_model_process = subprocess.Popen(
            [
                sys.executable,
                AsyncGrpcTestCase.SOCKET_WORKER_PATH,
                AsyncGrpcTestCase.EXISTING_MODEL_WORKER,
                "localhost",
                str(self._get_port()),
            ]
        )

    @staticmethod
    async def _add_services(grpc_server: Server, server_config: ServerConfig):
        model_storage = ModelStorage(
            server_config=server_config,
        )
        await model_storage.load_models()

        inference_service = InferenceService(model_storage=model_storage)

        inference_grpc = InferenceServicerPort(inference_service)
        inference_pb2_grpc.add_InferenceServiceServicer_to_server(
            inference_grpc, grpc_server
        )

    async def asyncTearDown(self):
        await self.channel.close()
        await self.server.stop(None)
        self.test_model_process.terminate()

    def get_stub(self) -> InferenceServiceStub:
        return InferenceServiceStub(self.channel)
