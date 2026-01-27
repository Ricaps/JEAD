import asyncio
import json
import os
import struct
import sys
from pathlib import Path
from typing import Optional
from importlib import util


shutdown_event = asyncio.Event()

WORKER_CLASS = "MLWorker"

LOAD_METHOD = "load"
UNLOAD_METHOD = "unload"
EXECUTE_METHOD = "execute"


def _load_worker_module(path: Path):
    script = util.spec_from_file_location("worker_module", path)
    module = util.module_from_spec(script)
    sys.modules["worker_module"] = module
    script.loader.exec_module(module)

    return module


def create_worker(path: Path):
    module = _load_worker_module(path)

    if not hasattr(module, WORKER_CLASS):
        raise NameError(f"The worker is missing class {WORKER_CLASS} at {path}")

    worker = module.MLWorker()
    for method_name in (LOAD_METHOD, UNLOAD_METHOD, EXECUTE_METHOD):
        if not hasattr(worker, method_name):
            raise NameError(
                f"The class {WORKER_CLASS} doesn't have method {method_name}"
            )

    worker.load()

    return worker


async def recv_msg(reader: asyncio.StreamReader) -> Optional[bytes]:
    raw_len = await reader.readexactly(4)
    msg_len = struct.unpack("!I", raw_len)[0]

    data = await reader.readexactly(msg_len)
    return data


async def send(writer: asyncio.StreamWriter, message):
    print("Sending response")
    message = json.dumps(message).encode()
    payload = struct.pack("!I", len(message)) + message

    writer.write(payload)
    await writer.drain()
    print("Sending response")


def compose_response(message_id: str, success=True, error=None, data=None):
    if data is None:
        data = {}

    return {"message_id": message_id, "success": success, "error": error, "data": data}


async def handle(reader: asyncio.StreamReader, writer: asyncio.StreamWriter, worker):
    try:
        while True:
            raw_message = await recv_msg(reader)
            message = raw_message.decode()
            json_message = json.loads(message)

            print(json_message)
            if "message_id" not in json_message:
                raise RuntimeError("Socket message must have an `id` attribute!")

            message_id = json_message["message_id"]
            if "message" not in json_message:
                await send(
                    writer,
                    compose_response(
                        message_id, success=False, error="No data in request!"
                    ),
                )
                continue
            message = json_message["message"]

            if message["command"] == "shutdown":
                worker.unload()
                await send(writer, compose_response(message_id, success=True))

                shutdown_event.set()
                break

            elif message["command"] == "inference":
                try:
                    model_response = worker.execute(message["data"])
                    await send(
                        writer,
                        compose_response(message_id, success=True, data=model_response),
                    )
                except Exception as e:
                    await send(
                        writer,
                        compose_response(message_id, success=False, error=str(e)),
                    )

            elif message["command"] == "load":
                worker.load()
                await send(writer, compose_response(message_id, success=True))

            else:
                await send(
                    writer,
                    compose_response(
                        message_id, success=False, error="Unknown command!"
                    ),
                )

    except (asyncio.IncompleteReadError, ConnectionResetError) as e:
        print(e)

    finally:
        writer.close()
        await writer.wait_closed()


async def start_server(worker_path: Path, host: str, port: int):
    worker = create_worker(worker_path)

    server = await asyncio.start_server(
        lambda reader, writer: handle(reader, writer, worker), host, port
    )

    async with server:
        await shutdown_event.wait()

        server.close()
        await server.wait_closed()


if __name__ == "__main__":
    print("Starting...")

    env_arguments = {
        "worker-path": os.getenv("WORKER_PATH"),
        "model-host": os.getenv("MODEL_HOST"),
        "model-port": os.getenv("MODEL_PORT"),
    }

    arguments = sys.argv

    for default_arg in env_arguments.keys():
        for cmd_arg in arguments:
            if cmd_arg.startswith(f"--{default_arg}="):
                env_arguments[default_arg] = cmd_arg.split("=")[1]

    asyncio.run(
        start_server(
            Path(env_arguments["worker-path"]),
            env_arguments["model-host"],
            int(env_arguments["model-port"]),
        )
    )
