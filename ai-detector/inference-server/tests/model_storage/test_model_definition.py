from typing import Optional
from unittest.async_case import IsolatedAsyncioTestCase

from aiopath import AsyncPath

from inference_server.business.model_storage import ModelDefinition
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)

MODEL_ROOT_PATH = "./tests/resources/model_root/existing-model"


class DummyInferenceModel(InferenceModel):
    async def on_load(self): ...

    async def on_unload(self): ...
    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]: ...


class TestModelDefinition(IsolatedAsyncioTestCase):
    async def test_load_model(self):
        model_definition = self._create_dummy_definition()
        await model_definition.load_model()

        self.assertTrue(model_definition.is_loaded())

        # the instance is the same after calling load_model() twice
        await model_definition.load_model()
        self.assertTrue(model_definition.is_loaded())

    async def test_unload_model(self):
        model_definition = self._create_dummy_definition()
        self.assertFalse(model_definition.is_loaded())

        await model_definition.load_model()

        self.assertTrue(model_definition.is_loaded())

        await model_definition.unload_model()
        self.assertFalse(model_definition.is_loaded())

    @staticmethod
    def _create_dummy_definition():
        return ModelDefinition(
            AsyncPath("tests/resources/model_root"), AsyncPath(MODEL_ROOT_PATH)
        )
