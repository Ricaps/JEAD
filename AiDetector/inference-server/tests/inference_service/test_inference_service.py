from unittest import TestCase
from unittest.mock import Mock, MagicMock

from inference_server.business.inference_service import InferenceService
from inference_server.business.model_storage import ModelStorage, ModelDefinition
from inference_server.exception.model import ModelNotExistsException
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)


class TestInferenceService(TestCase):
    def test_executeRequestNotExistingModel(self):
        storage = self._createDummyStorage()
        storage.get_model = MagicMock()
        storage.get_model.return_value = None

        service = self._create_service(storage)

        self.assertRaises(
            ModelNotExistsException, lambda: service.execute_request(Mock())
        )

    def test_executeRequestUnloadedModel_called(self):
        storage = self._createDummyStorage()
        storage.get_model = MagicMock()

        model_inference_result = Mock(spec=ModelInferenceResultBatch)

        model = Mock(spec=ModelDefinition)
        model.execute.return_value = model_inference_result
        model.is_loaded.return_value = True
        storage.get_model.return_value = model

        service = self._create_service(storage)
        mock_request = Mock(spec=ModelInferenceRequestBatch)
        mock_request.model_name = "model_name"

        response = service.execute_request(mock_request)

        model.execute.assert_called_once()
        model.load_model().assert_not_called()
        self.assertEqual(response, model_inference_result)

    def test_executeRequestLoadedModel_called(self):
        storage = self._createDummyStorage()
        storage.get_model = MagicMock()

        model_inference_result = Mock(spec=ModelInferenceResultBatch)

        model = Mock(spec=ModelDefinition)
        model.execute.return_value = model_inference_result
        model.is_loaded.return_value = False
        storage.get_model.return_value = model

        service = self._create_service(storage)
        mock_request = Mock(spec=ModelInferenceRequestBatch)
        mock_request.model_name = "model_name"

        response = service.execute_request(mock_request)

        model.execute.assert_called_once()
        model.load_model.assert_called_once()
        self.assertEqual(response, model_inference_result)

    def test_unloadLoadedModel(self):
        storage = self._createDummyStorage()
        model_mock = MagicMock()
        storage.get_model = MagicMock(return_value=model_mock)
        model_mock.is_loaded.return_value = True

        service = self._create_service(storage)
        model_name = "model"

        return_value = service.unload_model(model_name)

        storage.get_model.assert_called_once_with(model_name)
        model_mock.is_loaded.assert_called_once()
        model_mock.unload_model.assert_called_once()
        self.assertTrue(return_value)

    def test_unloadUnloadedModel(self):
        storage = self._createDummyStorage()
        model_mock = MagicMock()
        model_mock.is_loaded.return_value = False
        storage.get_model = MagicMock(return_value=model_mock)

        service = self._create_service(storage)
        model_name = "model"

        return_value = service.unload_model(model_name)

        storage.get_model.assert_called_once_with(model_name)
        model_mock.is_loaded.assert_called_once()
        model_mock.unload_model.assert_not_called()
        self.assertFalse(return_value)

    def test_loadUnloadedModel(self):
        storage = self._createDummyStorage()
        model_mock = MagicMock()
        model_mock.is_loaded.return_value = False
        storage.get_model = MagicMock(return_value=model_mock)

        service = self._create_service(storage)
        model_name = "model"

        return_value = service.load_model(model_name)

        storage.get_model.assert_called_once_with(model_name)
        model_mock.is_loaded.assert_called_once()
        model_mock.load_model.assert_called_once()
        self.assertTrue(return_value)

    def test_loadLoadedModel(self):
        storage = self._createDummyStorage()
        model_mock = MagicMock()
        model_mock.is_loaded.return_value = True
        storage.get_model = MagicMock(return_value=model_mock)

        service = self._create_service(storage)
        model_name = "model"

        return_value = service.load_model(model_name)

        storage.get_model.assert_called_once_with(model_name)
        model_mock.is_loaded.assert_called_once()
        model_mock.load_model.assert_not_called()
        self.assertFalse(return_value)

    @staticmethod
    def _create_service(storage):
        return InferenceService(storage)

    @staticmethod
    def _createDummyStorage():
        return ModelStorage(Mock(), Mock())
