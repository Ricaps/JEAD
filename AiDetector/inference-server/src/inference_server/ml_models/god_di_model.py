import asyncio
import dataclasses
import json

import numpy as np
from aiopath import AsyncPath
from typing import Optional, Any
import logging

from onnxruntime import InferenceSession, get_available_providers, preload_dlls

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
    ModelInferenceResult,
    LabelEvaluation,
    ModelInferenceRequest,
)


@dataclasses.dataclass
class InputRequest:
    """The order of elements must be kept! The model depends on it."""

    noom: int
    nooa: int
    nocm: int
    LCOM5: float
    cc: int
    loc: int


class GodDiModel(InferenceModel):
    SUBFOLDER_NAME = "onnx"
    labels = {0: "clean", 1: "god_di"}

    def __init__(self, model_root_path: AsyncPath):
        super().__init__(model_root_path)
        self.session: Optional[InferenceSession] = None
        self._access_lock = asyncio.Lock()
        self.__logger = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def map_json_content(request: ModelInferenceRequest):
        content = request.content
        json_content = json.loads(content)

        return GodDiModel.get_model_input(InputRequest(**json_content))

    @staticmethod
    def get_model_input(request: InputRequest):
        return [getattr(request, field.name) for field in dataclasses.fields(request)]

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        inputs = np.array(
            [GodDiModel.map_json_content(content) for content in data.contents],
            dtype=np.float32,
        )

        async with self._access_lock:
            raw_results = self.session.run(None, {"inputs": inputs})[0]

        results: list[ModelInferenceResult] = []

        for index, result in enumerate(raw_results):
            result_id = data.contents[index].id
            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, result)

            label_evaluation = LabelEvaluation(
                label=GodDiModel.labels[result], score=result
            )
            results.append(
                ModelInferenceResult(id=result_id, label_evaluation=[label_evaluation])
            )

        return ModelInferenceResultBatch(contents=results)

    @staticmethod
    def _process_result(label: dict[str, Any]) -> LabelEvaluation:
        return LabelEvaluation(**{**label, "score": round(label["score"], 5)})

    async def on_unload(self):
        async with self._access_lock:
            self.session = None

    async def on_load(self):
        self.__logger.info(f"Available providers: {get_available_providers()}")
        preload_dlls()
        path = self._model_root_path.joinpath(GodDiModel.SUBFOLDER_NAME)
        async with self._access_lock:
            self.session = InferenceSession(
                path.joinpath("model.onnx"),
                providers=["CPUExecutionProvider"],
            )
