from abc import ABC, abstractmethod
from typing import Optional

from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
)


class InferenceModelExecutable(ABC):
    @abstractmethod
    def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]: ...


class InferenceModel(InferenceModelExecutable, ABC):
    @abstractmethod
    def on_load(self): ...

    @abstractmethod
    def on_unload(self): ...
