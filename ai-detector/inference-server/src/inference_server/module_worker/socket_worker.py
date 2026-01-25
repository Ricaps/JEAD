import asyncio
import json
import struct
import sys
from pathlib import Path
from typing import Optional

from inference_server.module_worker.load_worker import create_worker

shutdown_event = asyncio.Event()


async def recv_msg(reader: asyncio.StreamReader) -> Optional[bytes]:
    raw_len = await reader.readexactly(4)
    msg_len = struct.unpack("!I", raw_len)[0]

    data = await reader.readexactly(msg_len)
    return data


async def send(writer: asyncio.StreamWriter, message):
    message = json.dumps(message).encode()
    payload = struct.pack("!I", len(message)) + message

    writer.write(payload)
    await writer.drain()


async def handle(reader: asyncio.StreamReader, writer: asyncio.StreamWriter, worker):
    try:
        while True:
            raw_message = await recv_msg(reader)
            message = raw_message.decode()
            json_message = json.loads(message)

            print(json_message)
            if json_message["command"] == "shutdown":
                worker.unload()
                await send(writer, {"success": True, "data": {}})

                shutdown_event.set()
                break

            elif json_message["command"] == "inference":
                model_response = worker.execute(json_message["data"])

                await send(writer, {"success": True, "data": model_response})

            elif json_message["command"] == "load":
                worker.load()
                await send(writer, {"success": True, "data": {}})

            else:
                await send(writer, {"success": False, "data": {}})

    except (asyncio.IncompleteReadError, ConnectionResetError):
        pass

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
    path = sys.argv[1]
    host = sys.argv[2]
    port = sys.argv[3]

    asyncio.run(
        start_server(
            Path(path),
            "localhost",
            int(port),
        )
    )
