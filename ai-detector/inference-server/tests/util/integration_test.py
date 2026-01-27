import subprocess
import sys
from unittest import IsolatedAsyncioTestCase


class IntegrationTest(IsolatedAsyncioTestCase):
    EXISTING_MODEL_NAME = "existing-model"
    SOCKET_WORKER_PATH = "models/socket_worker.py"
    EXISTING_MODEL_WORKER = "tests/resources/existing_model/worker.py"

    async def asyncSetUp(self):
        self.test_model_process = subprocess.Popen(
            [
                sys.executable,
                IntegrationTest.SOCKET_WORKER_PATH,
                f"--worker-path={IntegrationTest.EXISTING_MODEL_WORKER}",
                "--model-host=localhost",
                "--model-port=5555",
            ]
        )

    async def asyncTearDown(self):
        self.test_model_process.terminate()
