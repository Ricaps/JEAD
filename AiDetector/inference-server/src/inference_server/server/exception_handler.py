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
        response_or_iterator = method(request_or_iterator, context)
        try:
            if not hasattr(response_or_iterator, "__aiter__"):
                return await response_or_iterator
        except BaseServerException as exception:
            self._handle_exception(exception, context)

        return self._handle_iterator(response_or_iterator, context)


    async def _handle_iterator(self, iterator, context):
        try:
            async for i in iterator:
                yield i
        except BaseServerException as exception:
            self._handle_exception(exception, context)

    def _handle_exception(self, exception: BaseServerException, context):
        context.set_code(exception.status_code)
        context.set_details(exception.message)
        self.__logger.error(exception, exc_info=True)
