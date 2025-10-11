import asyncio

import numpy as np
from aiopath import AsyncPath
from typing import Optional, Any, Callable
import logging

from onnxruntime import InferenceSession

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
    ModelInferenceResult,
    LabelEvaluation,
)
from transformers import (
    AutoTokenizer,
)


class CommentsModel(InferenceModel):
    SUBFOLDER_NAME = "model"
    labels = {0: "clean", 1: "code_comment", 2: "technical_debt"}

    def __init__(self, model_root_path: AsyncPath):
        super().__init__(model_root_path)
        self.tokenizer: Optional[Callable[[Any], Any]] = None
        self.session: Optional[InferenceSession] = None
        self._access_lock = asyncio.Lock()
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        mapped = map(lambda request: request.content, data.contents)

        async with self._access_lock:
            inputs = self.tokenizer(
                list(mapped), return_tensors="np", padding=True, truncation=True
            )
            raw_results = self.session.run(None, {k: v for k, v in inputs.items()})

        results: list[ModelInferenceResult] = []
        logits = raw_results[0]
        probabilities = np.exp(logits) / np.exp(logits).sum(axis=1, keepdims=True)

        for index, prob in enumerate(probabilities):
            result_id = data.contents[index].id
            result = self._get_probabilities_per_label(prob)
            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, result)

            results.append(ModelInferenceResult(id=result_id, label_evaluation=result))

        return ModelInferenceResultBatch(contents=results)

    def _get_probabilities_per_label(self, probabilities):
        results = []
        for index, probability in enumerate(probabilities):
            results.append(
                LabelEvaluation(label=self.labels[index], score=round(probability, 10))
            )

        return results

    @staticmethod
    def _process_result(label: dict[str, Any]) -> LabelEvaluation:
        return LabelEvaluation(**{**label, "score": round(label["score"], 5)})

    async def on_unload(self):
        async with self._access_lock:
            self.tokenizer = None
            self.session = None

    async def on_load(self):
        path = self._model_root_path.joinpath(CommentsModel.SUBFOLDER_NAME)
        async with self._access_lock:
            self.tokenizer = AutoTokenizer.from_pretrained(path)
            self.session = InferenceSession(
                path.joinpath("model.onnx"),
                providers=["CUDAExecutionProvider", "CPUExecutionProvider"],
            )
