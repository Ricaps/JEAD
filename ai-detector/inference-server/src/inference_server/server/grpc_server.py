import asyncio
import signal
from typing import Any
import logging
from grpc.aio import Server, server
from grpc_reflection.v1alpha import reflection

from inference_server.business.model_storage import ModelStorage
from inference_server.business.shutdown_aware import ShutdownAware
from inference_server.configuration.config import ServerConfig
from inference_server.business.inference_service import InferenceService
from inference_server.proto import inference_pb2_grpc, inference_pb2
from inference_server.grpc_service.inference_port import InferenceServicerPort
from inference_server.server.exception_handler import ExceptionHandlerInterceptor

__LOGGER = logging.getLogger(__name__)

__shutdown_listeners: list[ShutdownAware] = []


async def __add_services(grpc_server: Server, server_config: ServerConfig):
    model_storage = ModelStorage(server_config=server_config)
    await model_storage.load_models()

    inference_service = InferenceService(model_storage=model_storage)

    inference_grpc = InferenceServicerPort(inference_service)
    inference_pb2_grpc.add_InferenceServiceServicer_to_server(
        inference_grpc, grpc_server
    )

    __shutdown_listeners.append(model_storage)


def __extract_services(services: dict[str, Any]) -> list[str]:
    return list(map(lambda service: service.full_name, services.values()))


def __setup_reflection(grpc_server: Server):
    service_names = (
        *__extract_services(inference_pb2.DESCRIPTOR.services_by_name),
        reflection.SERVICE_NAME,
    )

    reflection.enable_server_reflection(service_names, grpc_server)


def __get_interceptors():
    return [ExceptionHandlerInterceptor()]


async def create_server() -> Server:
    grpc_server = server(interceptors=__get_interceptors())
    __setup_reflection(grpc_server)

    return grpc_server


async def _notify_shutdown_listeners() -> None:
    for listener in __shutdown_listeners:
        await listener.on_shutdown()


async def _wait_on_termination():
    stop_event = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGTERM, signal.SIGINT):
        loop.add_signal_handler(sig, stop_event.set)
    await stop_event.wait()


async def run_and_wait():
    """
    Runs async.io GRPC server and waits until the termination
    """
    server_config = ServerConfig()
    grpc_server = await create_server()
    await __add_services(grpc_server, server_config)

    address = f"{server_config.address}:{str(server_config.port)}"
    __LOGGER.info("Starting GRPC server at %s", address)
    grpc_server.add_insecure_port(address)
    await grpc_server.start()

    await _wait_on_termination()

    __LOGGER.info("Shutting down GRPC server...")
    await grpc_server.stop(None)
    await _notify_shutdown_listeners()
