from typing import Any
import logging
from grpc.aio import Server, server
from grpc_reflection.v1alpha import reflection

from inference_server.business.model_storage import ModelStorage
from inference_server.configuration.config import server_config
from inference_server.business.inference_service import InferenceService
from inference_server.ml_models import model_type_registry
from inference_server.proto import inference_pb2_grpc, inference_pb2
from inference_server.grpc_service.inference_port import InferenceServicerPort
from inference_server.server.exception_handler import ExceptionHandlerInterceptor

__LOGGER = logging.getLogger(__name__)


async def __add_services(grpc_server: Server):
    model_storage = ModelStorage(
        server_config=server_config, model_type_registry=model_type_registry
    )
    await model_storage.load_models()

    inference_service = InferenceService(model_storage=model_storage)

    inference_grpc = InferenceServicerPort(inference_service)
    inference_pb2_grpc.add_InferenceServiceServicer_to_server(
        inference_grpc, grpc_server
    )


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
    await __add_services(grpc_server)
    __setup_reflection(grpc_server)

    return grpc_server


async def run_and_wait():
    """
    Runs async.io GRPC server and waits until the termination
    """
    grpc_server = await create_server()

    address = f"{server_config.address}:{str(server_config.port)}"
    __LOGGER.info("Starting GRPC server at %s", address)
    grpc_server.add_insecure_port(address)
    await grpc_server.start()
    await grpc_server.wait_for_termination()
