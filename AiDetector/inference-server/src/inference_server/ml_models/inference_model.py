from abc import ABC, abstractmethod
from pathlib import Path
from typing import Optional, Final

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
    def __init__(self, model_root_path: Path):
        self._model_root_path: Final[Path] = model_root_path

    @abstractmethod
    def on_load(self): ...

    @abstractmethod
    def on_unload(self): ...
