from abc import ABC, abstractmethod
from typing import Optional, Final

from aiopath import AsyncPath

from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
)


class InferenceModelExecutable(ABC):
    @abstractmethod
    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]: ...


class InferenceModel(InferenceModelExecutable, ABC):
    def __init__(self, model_root_path: AsyncPath):
        self._model_root_path: Final[AsyncPath] = model_root_path

    @abstractmethod
    async def on_load(self): ...

    @abstractmethod
    async def on_unload(self): ...
