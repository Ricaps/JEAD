import asyncio

from aiopath import AsyncPath
from typing import Optional, Any
import logging

from inference_server.exception.model import ModelNotLoadedException
from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
    ModelInferenceResult,
    LabelEvaluation,
)
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TextClassificationPipeline,
)


class CommentsModel(InferenceModel):
    SUBFOLDER_NAME = "model"

    def __init__(self, model_root_path: AsyncPath):
        super().__init__(model_root_path)
        self.tokenizer: Optional[Any] = None
        self.model: Optional[Any] = None
        self.pipeline: Optional[TextClassificationPipeline] = None
        self._access_lock = asyncio.Lock()
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        mapped = map(lambda request: request.content, data.contents)

        async with self._access_lock:
            if self.pipeline is None:
                raise ModelNotLoadedException("comments-model")
            raw_results = self.pipeline(list(mapped), batch_size=1)

        results: list[ModelInferenceResult] = []
        for index, labels in enumerate(raw_results):
            labels: list[dict[str, Any]] = labels  # Type cast
            result_id = data.contents[index].id

            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, labels)
            label_evaluation = list(map(self._process_result, labels))

            print(label_evaluation)
            results.append(
                ModelInferenceResult(id=result_id, label_evaluation=label_evaluation)
            )

        return ModelInferenceResultBatch(contents=results)

    @staticmethod
    def _process_result(label: dict[str, Any]) -> LabelEvaluation:
        return LabelEvaluation(**{**label, "score": round(label["score"], 5)})

    async def on_unload(self):
        async with self._access_lock:
            self.tokenizer = None
            self.model = None
            self.pipeline = None

    async def on_load(self):
        path = self._model_root_path.joinpath(CommentsModel.SUBFOLDER_NAME)
        async with self._access_lock:
            self.tokenizer = AutoTokenizer.from_pretrained(path)
            self.model = AutoModelForSequenceClassification.from_pretrained(path)
            self.pipeline = TextClassificationPipeline(
                model=self.model,
                tokenizer=self.tokenizer,
                top_k=None,
                device=-1,
                truncation=True,
            )
