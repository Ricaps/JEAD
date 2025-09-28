from pathlib import Path
from typing import Optional, Any
import logging

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

    def __init__(self, model_root_path: Path):
        super().__init__(model_root_path)
        self.tokenizer: Optional[Any] = None
        self.model: Optional[Any] = None
        self.pipeline: Optional[TextClassificationPipeline] = None
        self.__logger = logging.getLogger(self.__class__.__name__)

    def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        mapped = map(lambda request: request.content, data.contents)
        raw_results = self.pipeline(list(mapped))

        results: list[ModelInferenceResult] = []
        for index, labels in enumerate(raw_results):
            result_id = data.contents[index].id

            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, labels)
            label_evaluation = map(lambda label: LabelEvaluation(**label), labels)

            results.append(
                ModelInferenceResult(
                    id=result_id, label_evaluation=list(label_evaluation)
                )
            )

        return ModelInferenceResultBatch(contents=results)

    def on_unload(self):
        del self.tokenizer
        del self.model

    def on_load(self):
        path = self._model_root_path.joinpath(CommentsModel.SUBFOLDER_NAME)
        self.tokenizer = AutoTokenizer.from_pretrained(path)
        self.model = AutoModelForSequenceClassification.from_pretrained(path)
        self.pipeline = TextClassificationPipeline(
            model=self.model, tokenizer=self.tokenizer, top_k=None, device=-1
        )
