import json
from typing import TypedDict, List, Type, TypeVar, Generic, Optional
from enum import Enum
from ollama import ChatResponse, AsyncClient
import logging
from pydantic import BaseModel, ValidationError


class Role(Enum):
    USER = "user"
    SYSTEM = "system"
    ASSISTANT = "assistant"

class Model(Enum):
    CODE_LLAMA_7b = "codellama:7b"
    CODE_LLAMA_7b_INSTRUCT = "codellama:7b-instruct"
    CODE_LLAMA_13b_INSTRUCT = "codellama:13b-instruct"
    LLAMA_3_1_8b = "llama3.1:8b"
    LLAMA_3_1 = "llama3.1"

class Message(TypedDict):
    role: str
    content: str

T = TypeVar('T', bound=BaseModel)
class OllamaConnector(Generic[T]):

    def __init__(self, model: Model, response_class: Type[T]):
        self.__model = model
        self.__messages: List[Message] = []
        self.response_class = response_class
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def init_session(self, init_prompt: str) -> str:
        """
        Inits the new session with the LLM
        Clears the previous session if it was started before
        :param init_prompt: starting prompt with instructions
        :return: Returned response for init prompt
        """

        self.clear_session()
        self.__messages.append({
            "role": Role.SYSTEM.value,
            "content": init_prompt
        })

        response = await self._send_to_llm()
        self.__logger.debug(f"Received response: {response}")

        return response

    def clear_session(self) -> None:
        """
        Clears current session
        :return: None
        """
        self.__logger.debug("Clearing context")
        self.__messages.clear()

    async def send(self, prompt: str) -> Optional[T]:
        """
        Sends a prompt to the LLM and returns response
        :param prompt: prompt to be sent
        :returns: str : response as string
        """
        self.__messages.append({
            "role": Role.USER.value,
            "content": json.dumps({"content": prompt})
        })

        self.__logger.info(f"Sending message: {prompt}")
        response = await self._send_to_llm_structured()

        self.__logger.info(f"Received response: {response}")

        return response

    async def _send_to_llm(self) -> str:
        response: ChatResponse = await AsyncClient().chat(model=self.__model.value, messages=self.__messages)

        response_content = response["message"]["content"]
        self.__messages.append({
            "role": Role.ASSISTANT.value,
            "content": response_content
        })

        return response_content

    async def _send_to_llm_structured(self) -> Optional[T]:
        response: ChatResponse = await AsyncClient().chat(model=self.__model.value, messages=self.__messages, format=self.response_class.model_json_schema())

        response_content = response["message"]["content"]
        self.__messages.append({
            "role": Role.ASSISTANT.value,
            "content": response_content
        })

        try:
            return self.response_class.model_validate_json(response_content)
        except ValidationError:
            return None