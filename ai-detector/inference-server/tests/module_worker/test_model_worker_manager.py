import subprocess
from unittest.async_case import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock

from aiopath import AsyncPath

from inference_server.exception.model import ModelTimeoutError, WorkerStatusException
from inference_server.module_worker.model_worker_manager import (
    Message,
    ModelWorkerManager,
    Response,
    WorkerCommand,
    WorkerStatus,
)
from util.test_config import create_test_server_config


class TestModelWorkerManager(IsolatedAsyncioTestCase):
    MODEL_PATH = AsyncPath("tests/resources/model_root/existing-model")

    def _create_manager(self) -> ModelWorkerManager:
        return ModelWorkerManager(self.MODEL_PATH, create_test_server_config())

    async def test_load_timeout_forces_shutdown(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.RUNNING
        manager._ModelWorkerManager__send_internal = AsyncMock(
            side_effect=ModelTimeoutError("timeout")
        )
        manager._shutdown_process = AsyncMock()

        with self.assertRaises(ModelTimeoutError):
            await manager.send_load_command()

        manager._shutdown_process.assert_awaited_once()
        self.assertFalse(manager.is_loaded)

    async def test_load_failed_response_forces_shutdown(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.RUNNING
        manager._ModelWorkerManager__send_internal = AsyncMock(
            return_value=Response(message_id="id", success=False, error="failed")
        )
        manager._shutdown_process = AsyncMock()

        response = await manager.send_load_command()

        self.assertFalse(response.success)
        manager._shutdown_process.assert_awaited_once()
        self.assertFalse(manager.is_loaded)

    async def test_is_loaded_only_after_successful_load_response(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.RUNNING
        manager._ModelWorkerManager__send_internal = AsyncMock(
            return_value=Response(message_id="id", success=True)
        )

        await manager.send_load_command()

        self.assertTrue(manager.is_loaded)

    async def test_shutdown_inactive_is_noop(self):
        manager = self._create_manager()
        manager._ModelWorkerManager__send_internal = AsyncMock()

        await manager.shutdown()

        manager._ModelWorkerManager__send_internal.assert_not_awaited()

    async def test_send_requires_running_worker(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.INACTIVE

        with self.assertRaises(WorkerStatusException):
            await manager.send(Message(command=WorkerCommand.INFERENCE, data={}))

    async def test_send_load_command_requires_running_worker(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.LOADING

        with self.assertRaises(WorkerStatusException):
            await manager.send_load_command()

    async def test_send_internal_timeout_cleans_pending_request(self):
        manager = self._create_manager()
        manager._ModelWorkerManager__model_command_timeout = 0.001
        manager._send_message = AsyncMock()

        with self.assertRaises(ModelTimeoutError):
            await manager._ModelWorkerManager__send_internal(
                Message(command=WorkerCommand.INFERENCE, data={})
            )

        self.assertEqual({}, manager._ModelWorkerManager__pending_requests)
        self.assertTrue(manager._ModelWorkerManager__processing_finished.is_set())

    async def test_shutdown_loading_forces_process_cleanup(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.LOADING
        manager._ModelWorkerManager__send_internal = AsyncMock()
        manager._shutdown_process = AsyncMock()

        await manager.shutdown()

        manager._ModelWorkerManager__send_internal.assert_not_awaited()
        manager._shutdown_process.assert_awaited_once()

    async def test_shutdown_running_sends_shutdown_command(self):
        manager = self._create_manager()
        manager._status = WorkerStatus.RUNNING
        manager._ModelWorkerManager__send_internal = AsyncMock(
            return_value=Response(message_id="id", success=True)
        )
        manager._ModelWorkerManager__processing_finished.set()
        manager._shutdown_process = AsyncMock()

        await manager.shutdown()

        manager._ModelWorkerManager__send_internal.assert_awaited_once()
        manager._shutdown_process.assert_awaited_once()

    def test_wait_or_force_stop_process_terminate_path(self):
        class FakeProcess:
            def __init__(self):
                self.terminate_called = False
                self.kill_called = False
                self.wait_calls = 0

            def wait(self, timeout):
                self.wait_calls += 1
                if self.wait_calls == 1:
                    raise subprocess.TimeoutExpired(cmd="worker", timeout=timeout)
                return 0

            def terminate(self):
                self.terminate_called = True

            def kill(self):
                self.kill_called = True

        manager = self._create_manager()
        fake_process = FakeProcess()
        manager._ModelWorkerManager__process = fake_process

        manager._wait_or_force_stop_process()

        self.assertTrue(fake_process.terminate_called)
        self.assertFalse(fake_process.kill_called)

    def test_wait_or_force_stop_process_kill_path(self):
        class FakeProcess:
            def __init__(self):
                self.terminate_called = False
                self.kill_called = False
                self.wait_calls = 0

            def wait(self, timeout):
                self.wait_calls += 1
                if self.wait_calls <= 2:
                    raise subprocess.TimeoutExpired(cmd="worker", timeout=timeout)
                return 0

            def terminate(self):
                self.terminate_called = True

            def kill(self):
                self.kill_called = True

        manager = self._create_manager()
        fake_process = FakeProcess()
        manager._ModelWorkerManager__process = fake_process

        manager._wait_or_force_stop_process()

        self.assertTrue(fake_process.terminate_called)
        self.assertTrue(fake_process.kill_called)
