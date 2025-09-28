from typing import Callable, Awaitable

import grpc
import logging
from grpc.aio import ServerInterceptor

from inference_server.exception import BaseServerException


class ExceptionHandlerInterceptor(ServerInterceptor):
    def __init__(self):
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def intercept_service(
        self,
        continuation: Callable[
            [grpc.HandlerCallDetails], Awaitable[grpc.RpcMethodHandler]
        ],
        handler_call_details: grpc.HandlerCallDetails,
    ) -> grpc.RpcMethodHandler:
        handler = await continuation(handler_call_details)

        def wrapper(behavior):
            def new_behavior(request, context):
                try:
                    return behavior(request, context)
                except BaseServerException as exception:
                    context.set_code(exception.status_code)
                    context.set_details(exception.message)
                    self.__logger.error(exception, exc_info=True)

            return new_behavior

        return grpc.unary_unary_rpc_method_handler(
            wrapper(handler.unary_unary),
            request_deserializer=handler.request_deserializer,
            response_serializer=handler.response_serializer,
        )
