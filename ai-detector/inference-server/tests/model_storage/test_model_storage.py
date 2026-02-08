from typing import Optional
from unittest.async_case import IsolatedAsyncioTestCase


from inference_server.business.model_storage import ModelStorage, ModelDefinition
from inference_server.configuration.config import ServerConfig
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)


class DummyInferenceModel(InferenceModel):
    async def on_unload(self): ...
    async def on_load(self): ...
    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]: ...


class TestModelStorage(IsolatedAsyncioTestCase):
    EXISTING_MODEL_NAME = "existing-model"
    EXISTING_MODEL_NO_FOLDER_NAME = "existing-model-no-folder"
    EXISTING_MODEL_NO_REGISTRY_NAME = "existing-no-registry"

    async def test_get_existing_model(self):
        storage = self._createDummyStorage()
        model = storage.get_model(self.EXISTING_MODEL_NAME)
        self.assertIsNone(model)

        await storage.load_models()

        model = storage.get_model(self.EXISTING_MODEL_NAME)
        self.assertIsNotNone(model)
        self.assertIsInstance(model, ModelDefinition)

        await model.load_model()
        self.assertTrue(model.is_loaded())

    async def test_get_existing_model_no_folder(self):
        storage = self._createDummyStorage()
        await storage.load_models()

        model = storage.get_model(self.EXISTING_MODEL_NO_FOLDER_NAME)
        self.assertIsNone(model)

    async def test_get_model_with_folder_no_registry(self):
        storage = self._createDummyStorage()
        await storage.load_models()

        model = storage.get_model(self.EXISTING_MODEL_NO_REGISTRY_NAME)
        self.assertIsNone(model)

    @staticmethod
    def _createDummyStorage():
        config = ServerConfig(
            address="0.0.0.0", port="8888", models_root="tests/resources/model_root"
        )

        return ModelStorage(config)
