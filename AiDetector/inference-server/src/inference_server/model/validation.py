from typing import TypeVar

from pydantic import BaseModel, ValidationError

from inference_server.exception.model import ContentNotValidException

Model = TypeVar("Model", bound=BaseModel)


def validate_model_and_get(json_content: str, model_cls: type[Model]) -> Model:
    """
    Validates if passed json_content is valid Pydantic BaseModel and converts it into the model directly.

    :param json_content: json string to be validated
    :param model_cls: BaseModel subtype against which the validation should be performed
    :returns: Validated BaseModel subtype instance
    :raises ContentNotValidException: When the input is invalid. With correct gRPC StatusCode.
    """

    try:
        return model_cls.model_validate_json(json_content)
    except ValidationError as ex:
        raise ContentNotValidException(str(ex))
