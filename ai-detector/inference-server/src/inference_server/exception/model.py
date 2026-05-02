from grpc.beta.interfaces import StatusCode

from inference_server.exception import BaseServerException


class ModelError(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)


class ModelTimeoutError(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)


class ModelNotExistsException(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)


class ModelNotLoadedException(BaseServerException):
    def __init__(self, model_name: str):
        super().__init__(
            StatusCode.FAILED_PRECONDITION, f"Desired model is not loaded: {model_name}"
        )


class ContentNotValidException(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.INVALID_ARGUMENT, message)


class WorkerStatusException(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.FAILED_PRECONDITION, message)


class ModelInferenceException(BaseServerException):
    def __init__(self, message: str):
        super().__init__(StatusCode.INTERNAL, message)
