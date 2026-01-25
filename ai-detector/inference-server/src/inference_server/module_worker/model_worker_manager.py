import asyncio
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


class Command(BaseModel):
    command: str
    data: Any


class Response(BaseModel):
    success: bool
    data: Any


class ModelWorkerManager:
    VENV_FOLDER = ".venv"
    WORKER_FILE = "worker.py"

    def __init__(self, worker_dir: AsyncPath):
        self.__lock = asyncio.Lock()
        self.__worker_dir: AsyncPath = worker_dir
        self.__model_name: str = worker_dir.name
        self.__reader: Optional[asyncio.StreamReader] = None
        self.__writer: Optional[asyncio.StreamWriter] = None
        self.__started = False
        self.__process: Optional[subprocess.Popen[str]] = None
        self._stdout_thread: Optional[Thread] = None
        self._stderr_thread: Optional[Thread] = None
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

    async def _wait_until_connection(self, host, port, timeout=10):
        if not self.__lock.locked():
            raise RuntimeError("Lock must be locked for connection operation!")

        timeout_time = time.time() + timeout

        while time.time() < timeout_time:
            try:
                reader, writer = await asyncio.open_connection(host, port)
                self.__reader = reader
                self.__writer = writer

                return
            except OSError:
                await asyncio.sleep(0.2)

        raise TimeoutError("Worker was not loaded properly!")

    async def start_process(self):
        port = self._get_port()

        host = "localhost"
        venv_path = self.__worker_dir / self.VENV_FOLDER
        python_bin = venv_path / "bin" / "python3"

        if not await venv_path.exists():
            self.__logger.warning(f"Starting '{self.__model_name}' without its .venv!")
            python_bin = Path("python3")

        self.__logger.info(f"Starting model process at path {self.__worker_dir}")
        async with self.__lock:
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

            await self._wait_until_connection(host, port)
            self.__started = True

            self._stdout_thread = await self._create_out_thread(self.__process.stdout)
            self._stdout_thread.start()

            self._stderr_thread = await self._create_out_thread(self.__process.stderr)
            self._stderr_thread.start()

        self.__logger.info(f"Model process at {self.__worker_dir} started!")

    async def _create_out_thread(self, out: IO[AnyStr]):
        return threading.Thread(target=self._forward_stream, args=(out,), daemon=True)

    async def _send_message(self, message: Command):
        if not self.__lock.locked():
            raise RuntimeError("Lock must be locked for write operation!")

        json_message = message.model_dump_json().encode()
        self.__writer.write(struct.pack("!I", len(json_message)) + json_message)
        await self.__writer.drain()

    async def _read_message(self) -> Response:
        if not self.__lock.locked():
            raise RuntimeError("Lock must be locked for read operation!")

        raw_len = await self.__reader.readexactly(4)
        msg_len = struct.unpack("!I", raw_len)[0]

        data = await self.__reader.readexactly(msg_len)
        json_data = json.loads(data.decode())

        return Response(**json_data)

    async def send(self, command: Command) -> Response:
        async with self.__lock:
            return await self._send_no_lock(command)

    async def _send_no_lock(self, command: Command):
        if not self.__lock.locked():
            raise RuntimeError("Lock must be locked for send operation!")

        await self._send_message(command)
        return await self._read_message()

    async def shutdown(self):
        self.__logger.info(f"Shutting down model process at {self.__worker_dir}")
        async with self.__lock:
            response = await self._send_no_lock(Command(command="shutdown", data={}))

            if response.success:
                self.__writer.close()
                await self.__writer.wait_closed()
                self.__process.wait(timeout=5)
                self.__started = False

                self._stdout_thread.join(timeout=5)
                self._stderr_thread.join(timeout=5)
                self.__logger.info(f"Model process at {self.__worker_dir} shutdown!")

    def _forward_stream(self, stream: IO[AnyStr]):
        for line in iter(stream.readline, ""):
            self.__logger.info(line.rstrip("\n"))
        stream.close()

    @property
    def is_loaded(self) -> bool:
        return self.__started
