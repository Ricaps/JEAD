from typing import Type, TypeVar, Any

from google.protobuf.message import Message
from google.protobuf.json_format import MessageToDict, ParseDict
from pydantic import BaseModel

ModelType = TypeVar("ModelType", bound=BaseModel)


def grpc_to_model(grpc_msg, model_class: Type[ModelType]) -> ModelType:
    grpc_dict = MessageToDict(grpc_msg, preserving_proto_field_name=True)
    return model_class(**grpc_dict)


MessageType = TypeVar("MessageType", bound=Message)


def dict_to_grpc(model: dict[str, Any], grpc_message: type[MessageType]) -> MessageType:
    return ParseDict(model, grpc_message)


def model_to_grpc(model: BaseModel, grpc_message: type[MessageType]) -> MessageType:
    return ParseDict(model.model_dump(), grpc_message())
