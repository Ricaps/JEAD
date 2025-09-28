from typing import Callable, Any

import logging

from grpc import aio as grpc_aio
from grpc_interceptor import AsyncServerInterceptor


from inference_server.exception import BaseServerException


class ExceptionHandlerInterceptor(AsyncServerInterceptor):
    def __init__(self):
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def intercept(
        self,
        method: Callable,
        request_or_iterator: Any,
        context: grpc_aio.ServicerContext,
        method_name: str,
    ) -> Any:
        try:
            return method(request_or_iterator, context)
        except BaseServerException as exception:
            context.set_code(exception.status_code)
            context.set_details(exception.message)
            self.__logger.error(exception, exc_info=True)
