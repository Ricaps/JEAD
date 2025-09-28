from grpc import StatusCode


class BaseServerException(Exception):
    def __init__(self, status_code: StatusCode, message: str):
        super().__init__(message)
        self.status_code = status_code
        self.message = message
