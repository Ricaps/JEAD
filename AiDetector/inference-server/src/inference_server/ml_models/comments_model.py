from typing import Optional

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceResultBatch,
    ModelInferenceRequestBatch,
    ModelInferenceResult,
)


class CommentsModel(InferenceModel):
    def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        return ModelInferenceResultBatch(
            contents=[ModelInferenceResult(id="abc", label="abc")]
        )

    def on_unload(self):
        pass

    def on_load(self):
        pass
