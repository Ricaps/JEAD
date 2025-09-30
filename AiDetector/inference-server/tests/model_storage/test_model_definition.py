from typing import Optional
from unittest.async_case import IsolatedAsyncioTestCase

from aiopath import AsyncPath

from inference_server.business.model_storage import ModelDefinition
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)


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
        model = model_definition._model_reference()

        self.assertIsInstance(model, DummyInferenceModel)

        # the instance is the same after calling load_model() twice
        await model_definition.load_model()
        self.assertEqual(model, model_definition._model_reference())

    async def test_unload_model(self):
        model_definition = self._create_dummy_definition()
        self.assertIsNone(model_definition._model_reference)

        await model_definition.load_model()

        self.assertIsNotNone(model_definition._model_reference())

        await model_definition.unload_model()
        self.assertIsNone(model_definition._model_reference)

    @staticmethod
    def _create_dummy_definition():
        return ModelDefinition(AsyncPath("random-path"), DummyInferenceModel)
