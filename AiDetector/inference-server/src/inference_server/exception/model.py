from grpc.beta.interfaces import StatusCode

from inference_server.exception import BaseServerException


class WrongModelConfiguration(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)


class ModelNotExistsException(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)
