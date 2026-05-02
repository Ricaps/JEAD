import tempfile
from pathlib import Path
from typing import Optional
from unittest.async_case import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock

from aiopath import AsyncPath

from inference_server.business.model_storage import ModelStorage, ModelDefinition
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
)
from util.test_config import create_test_server_config


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

    async def test_load_models_skips_rediscovery_when_already_loaded(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "model-a").mkdir()
            (root / "model-a" / "worker.py").write_text("class MLWorker: pass")
            (root / "model-b").mkdir()
            (root / "model-b" / "worker.py").write_text("class MLWorker: pass")

            storage = ModelStorage(
                create_test_server_config(models_root=str(root), port="8080")
            )
            storage.ensure_venv = AsyncMock(return_value=True)
            storage._install_requirements = AsyncMock(return_value=True)

            await storage.load_models()
            self.assertIsNotNone(storage.get_model("model-a"))
            self.assertIsNotNone(storage.get_model("model-b"))
            self.assertEqual(storage.ensure_venv.await_count, 1)
            self.assertEqual(storage._install_requirements.await_count, 1)

            (root / "model-a" / "worker.py").unlink()
            (root / "model-a").rmdir()

            await storage.load_models()
            self.assertIsNotNone(storage.get_model("model-a"))
            self.assertIsNotNone(storage.get_model("model-b"))
            self.assertEqual(storage.ensure_venv.await_count, 1)
            self.assertEqual(storage._install_requirements.await_count, 1)

    async def test_on_shutdown_continues_when_one_model_unload_fails(self):
        storage = self._createDummyStorage()
        failing_model = AsyncMock()
        failing_model.name = "failing-model"
        failing_model.unload_model.side_effect = RuntimeError("fail")
        healthy_model = AsyncMock()
        healthy_model.name = "healthy-model"

        storage._ModelStorage__model_holder = {
            "failing-model": failing_model,
            "healthy-model": healthy_model,
        }

        await storage.on_shutdown()

        failing_model.unload_model.assert_awaited_once()
        healthy_model.unload_model.assert_awaited_once()

    async def test_ensure_venv_returns_false_when_python_executable_missing(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = AsyncPath(tmp_dir)
            venv_path = root / ".venv"
            python_path = venv_path / "Scripts" / "python.exe"
            pip_path = venv_path / "Scripts" / "pip.exe"
            get_pip_path = venv_path / "get-pip.py"

            storage = ModelStorage(
                create_test_server_config(models_root=tmp_dir, port="8080")
            )
            storage._path_resolver.get_python_paths = lambda: (
                venv_path,
                python_path,
                pip_path,
            )
            storage._path_resolver.get_pip_script_path = lambda: get_pip_path
            storage._download_file = AsyncMock(return_value=None)

            result = await storage.ensure_venv(root)

            self.assertFalse(result)

    async def test_install_requirements_returns_false_when_requirements_missing(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = AsyncPath(tmp_dir)
            venv_path = root / ".venv"
            scripts_path = Path(tmp_dir) / ".venv" / "Scripts"
            scripts_path.mkdir(parents=True, exist_ok=True)
            (scripts_path / "pip.exe").write_text("")

            pip_path = venv_path / "Scripts" / "pip.exe"
            python_path = venv_path / "Scripts" / "python.exe"
            missing_requirements = root / "requirements.txt"

            storage = ModelStorage(
                create_test_server_config(models_root=tmp_dir, port="8080")
            )
            storage._path_resolver.get_python_paths = lambda: (
                venv_path,
                python_path,
                pip_path,
            )
            storage._path_resolver.get_requirements_path = lambda: missing_requirements

            result = await storage._install_requirements(root)

            self.assertFalse(result)

    @staticmethod
    def _createDummyStorage():
        return ModelStorage(create_test_server_config())
