from pathlib import Path
from typing import Optional
from unittest import TestCase

from inference_server.business.model_storage import ModelDefinition
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)


class DummyInferenceModel(InferenceModel):
    def on_load(self): ...

    def on_unload(self): ...
    def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]: ...


class TestModelDefinition(TestCase):
    def test_load_model(self):
        model_definition = self._create_dummy_definition()
        model_definition.load_model()
        model = model_definition._model_reference()

        self.assertIsInstance(model, DummyInferenceModel)

        # the instance is the same after calling load_model() twice
        model_definition.load_model()
        self.assertEqual(model, model_definition._model_reference())

    def test_unload_model(self):
        model_definition = self._create_dummy_definition()
        self.assertIsNone(model_definition._model_reference)

        model_definition.load_model()

        self.assertIsNotNone(model_definition._model_reference())

        model_definition.unload_model()
        self.assertIsNone(model_definition._model_reference)

    @staticmethod
    def _create_dummy_definition():
        return ModelDefinition(Path("random-path"), DummyInferenceModel)
