import asyncio
import uuid
from enum import Enum
import json
import logging
import struct
import subprocess
import threading
import time
from pathlib import Path
from threading import Thread
from typing import Optional, Any, IO, AnyStr

from aiopath import AsyncPath
from pydantic import BaseModel

from inference_server.exception.model import WorkerStatusException


class WorkerCommand(Enum):
    SHUTDOWN = "shutdown"
    INFERENCE = "inference"
    LOAD = "load"


class Message(BaseModel):
    """
    Model containing desired command to be sent with data passed to the model
    """

    command: WorkerCommand
    data: Any


class MessageDto(BaseModel):
    """
    Wrapper around Message model enhanced with message_id, which servers as and identifier for
    asynchronous processing of response
    """

    message_id: str
    message: Message


class Response(BaseModel):
    """
    Response model delivered back from the model worker
    """

    message_id: str
    success: bool
    data: Any = {}
    error: str | None = None


class WorkerStatus(Enum):
    RUNNING = 1
    SHUTTING_DOWN = 2
    LOADING = 3
    INACTIVE = 4


class ModelWorkerManager:
    VENV_FOLDER = ".venv"
    WORKER_FILE = "worker.py"
    REQUIREMENTS_FILE = "requirements.txt"

    def __init__(self, worker_dir: AsyncPath):
        self.__worker_dir: AsyncPath = worker_dir
        self.__model_name: str = worker_dir.name
        self.__reader: Optional[asyncio.StreamReader] = None
        self.__writer: Optional[asyncio.StreamWriter] = None
        self.__process: Optional[subprocess.Popen[str]] = None
        self._stdout_thread: Optional[Thread] = None
        self._stderr_thread: Optional[Thread] = None
        self.__reader_task: Optional[asyncio.Task] = None
        self.__pending_requests: dict[str, asyncio.Future] = {}
        self.__processing_finished = asyncio.Event()
        self.__processing_finished.set()
        self._status: WorkerStatus = WorkerStatus.INACTIVE
        self.__logger = logging.getLogger(
            self.__class__.__name__ + f"#{self.__model_name}"
        )

    @staticmethod
    def _get_port():
        import socket

        sock = socket.socket()
        sock.bind(("", 0))
        port = sock.getsockname()[1]
        sock.close()

        return port

    async def _connect_and_wait(self, host, port, timeout=120):
        timeout_time = time.time() + timeout

        while time.time() < timeout_time:
            try:
                reader, writer = await asyncio.open_connection(host, port)
                self.__reader = reader
                self.__writer = writer
                self.__reader_task = asyncio.get_running_loop().create_task(
                    self._read_loop()
                )

                return
            except OSError:
                await asyncio.sleep(0.2)

        raise TimeoutError("Worker was not loaded properly!")

    async def start_process(self):
        if self._status != WorkerStatus.INACTIVE:
            raise WorkerStatusException("Worker cannot be started when running!")

        self._status = WorkerStatus.LOADING
        port = self._get_port()

        host = "localhost"
        venv_path = self.__worker_dir / self.VENV_FOLDER
        python_bin = venv_path / "bin" / "python3"

        if not await venv_path.exists():
            self.__logger.warning(f"Starting '{self.__model_name}' without its .venv!")
            python_bin = Path("python3")

        self.__logger.info(f"Starting model process at path {self.__worker_dir}")
        self.__process = subprocess.Popen(
            [
                str(python_bin),
                "src/inference_server/module_worker/socket_worker.py",
                str(self.__worker_dir / self.WORKER_FILE),
                host,
                str(port),
            ],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        self._stdout_thread = await self._create_out_thread(self.__process.stdout)
        self._stdout_thread.start()

        self._stderr_thread = await self._create_out_thread(self.__process.stderr)
        self._stderr_thread.start()

        await self._connect_and_wait(host, port)
        self._status = WorkerStatus.RUNNING

        self.__logger.info(f"Model process at {self.__worker_dir} started!")

    async def _create_out_thread(self, out: IO[AnyStr]):
        return threading.Thread(target=self._forward_stream, args=(out,), daemon=True)

    async def _send_message(self, message: Message, message_id: str):
        command_dto = MessageDto(message_id=message_id, message=message)
        json_message = command_dto.model_dump_json().encode()
        self.__writer.write(struct.pack("!I", len(json_message)) + json_message)
        await self.__writer.drain()

    async def _read_loop(self):
        try:
            while True:
                raw_len = await self.__reader.readexactly(4)
                msg_len = struct.unpack("!I", raw_len)[0]

                data = await self.__reader.readexactly(msg_len)
                json_data = json.loads(data.decode())
                response = Response(**json_data)
                message_id = response.message_id

                if message_id not in self.__pending_requests:
                    self.__logger.warning(
                        f"Message with ID {message_id} was not found amount pending requests!"
                    )

                future = self.__pending_requests.pop(message_id)
                future.set_result(response)

                if self._status == WorkerStatus.INACTIVE:
                    break

        except asyncio.IncompleteReadError:
            self.__pending_requests.clear()
            self.__processing_finished.set()
            self.__logger.error("The connection was closed!")

    async def send(self, msg: Message) -> Response:
        if self._status != WorkerStatus.RUNNING:
            raise WorkerStatusException(
                "Cannot send request since the worker is not running!"
            )

        return await self.__send_internal(msg)

    async def __send_internal(self, msg: Message):
        future = asyncio.get_running_loop().create_future()
        message_id = uuid.uuid4().hex

        self.__processing_finished.clear()
        self.__pending_requests[message_id] = future
        await self._send_message(msg, message_id)

        return await asyncio.wait_for(future, timeout=30.0)

    async def shutdown(self):
        self.__logger.info(f"Shutting down model process at {self.__worker_dir}")

        self._status = WorkerStatus.SHUTTING_DOWN
        response = await self.__send_internal(
            Message(command=WorkerCommand.SHUTDOWN, data={})
        )

        await asyncio.wait_for(self.__processing_finished.wait(), timeout=10.0)

        if response.success:
            self.__writer.close()
            await self.__writer.wait_closed()
            self.__process.wait(timeout=5)

            self._status = WorkerStatus.INACTIVE

            self._stdout_thread.join(timeout=5)
            self._stderr_thread.join(timeout=5)

            await asyncio.wait_for(self.__reader_task, timeout=10.0)
            self.__reader_task.cancel()
            self.__logger.info(f"Model process at {self.__worker_dir} shutdown!")

    def _forward_stream(self, stream: IO[AnyStr]):
        for line in iter(stream.readline, ""):
            self.__logger.info(line.rstrip("\n"))
        stream.close()

    @property
    def is_loaded(self) -> bool:
        return self._status == WorkerStatus.RUNNING
