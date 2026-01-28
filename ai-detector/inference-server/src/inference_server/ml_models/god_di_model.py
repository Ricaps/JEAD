import asyncio

import numpy as np
from aiopath import AsyncPath
from typing import Optional, Any
import logging

from onnxruntime import InferenceSession
from pydantic import BaseModel

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
    ModelInferenceResult,
    LabelEvaluation,
    ModelInferenceRequest,
)
from inference_server.model.validation import validate_model_and_get
from inference_server.util.onnx_util import load_onnx


class InputRequest(BaseModel):
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
        input_request = validate_model_and_get(content, InputRequest)

        return GodDiModel.get_model_input(input_request)

    @staticmethod
    def get_model_input(request: InputRequest):
        return [value for value in request.model_dump().values()]

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        """
        Execute inference on God class / Data class detection using code metrics.

        Expected data structure (Pydantic model):
        ModelInferenceRequestBatch(
            model_name="god-di-model",
            contents=[
                ModelInferenceRequest(
                    id="unique-identifier",
                    content='{"noom": 10, "nooa": 5, "nocm": 8, "LCOM5": 0.75, "cc": 15, "loc": 200}'
                    # Note: content is a JSON string that will be parsed and validated
                )
            ]
        )

        After parsing content JSON string, it should contain these metrics:
        {
            "noom": int,  # Number of Methods
            "nooa": int,  # Number of Attributes
            "nocm": int,  # Number of Class Methods
            "LCOM5": float,  # Lack of Cohesion of Methods 5
            "cc": int,  # Cyclomatic Complexity
            "loc": int  # Lines of Code
        }

        Returns:
        ModelInferenceResultBatch(
            contents=[
                ModelInferenceResult(
                    id="unique-identifier",
                    label_evaluation=[
                        LabelEvaluation(label="clean", score=0.85)
                        # OR
                        LabelEvaluation(label="god_di", score=0.92)
                    ]
                )
            ]
        )
        """
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

            # Convert to float() to ensure JSON serializable (NumPy types are not)
            label_evaluation = LabelEvaluation(
                label=GodDiModel.labels[result], score=float(result)
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
        path = self._model_root_path.joinpath(GodDiModel.SUBFOLDER_NAME).joinpath(
            "model.onnx"
        )
        async with self._access_lock:
            self.session = load_onnx(path, False)
