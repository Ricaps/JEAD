import asyncio
import uuid
from enum import Enum
import json
import logging
import struct
import threading
import time
import socket
from typing import Optional, Any, IO, AnyStr

from pydantic import BaseModel

from inference_server.configuration.model_config import Model
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
    DISCONNECTING = 2
    LOADING = 3
    INACTIVE = 4


class ModelWorkerManager:
    VENV_FOLDER = ".venv"
    WORKER_FILE = "worker.py"

    def __init__(self, config: Model, name: str, host: str):
        self.__config: Model = config
        self.__model_name: str = name
        self.__host: str = host
        self.__reader: Optional[asyncio.StreamReader] = None
        self.__writer: Optional[asyncio.StreamWriter] = None
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
        sock = socket.socket()
        sock.bind(("", 0))
        port = sock.getsockname()[1]
        sock.close()

        return port

    async def _connect_and_wait(self, timeout=10):
        timeout_time = time.time() + timeout

        while time.time() < timeout_time:
            try:
                reader, writer = await asyncio.open_connection(
                    self.__host, self.__config.port
                )

                self.__reader = reader
                self.__writer = writer
                self.__reader_task = asyncio.get_running_loop().create_task(
                    self._read_loop()
                )

                return
            except OSError:
                await asyncio.sleep(0.2)

        raise TimeoutError("Worker was not loaded properly!")

    async def start_connection(self):
        if self._status != WorkerStatus.INACTIVE:
            raise WorkerStatusException("Worker cannot be started when running!")

        self._status = WorkerStatus.LOADING
        try:
            await self._connect_and_wait()
            self._status = WorkerStatus.RUNNING
            self.__logger.info(f"Model {self.__model_name} connected!")
        except TimeoutError:
            self.__logger.error(f"Model {self.__model_name} connection timeout!")
            self._status = WorkerStatus.INACTIVE
            raise WorkerStatusException(
                f"Failed to connect to the model worker {self.__model_name}!"
            )

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

                if len(self.__pending_requests) == 0:
                    self.__processing_finished.set()

                if self._status == WorkerStatus.INACTIVE:
                    break

        except asyncio.IncompleteReadError as e:
            self.__logger.error("Connection was closed!", e)
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
        self.__logger.info(f"Disconnecting model {self.__model_name}")

        self._status = WorkerStatus.DISCONNECTING
        await asyncio.wait_for(self.__processing_finished.wait(), timeout=10.0)

        self.__writer.close()
        await self.__writer.wait_closed()

        self._status = WorkerStatus.INACTIVE

        await asyncio.wait_for(self.__reader_task, timeout=10.0)
        self.__reader_task.cancel()
        self.__logger.info(f"Model {self.__model_name} disconnected!")

    def _forward_stream(self, stream: IO[AnyStr]):
        for line in iter(stream.readline, ""):
            self.__logger.info(line.rstrip("\n"))
        stream.close()

    @property
    def is_loaded(self) -> bool:
        return self._status == WorkerStatus.RUNNING
