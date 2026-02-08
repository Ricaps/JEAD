from typing import Optional

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
    ModelInferenceResult,
    LabelEvaluation,
    ModelInferenceRequest,
)

_TEST_LABEL_1 = "test_label_1"
_TEST_LABEL_2 = "test_label_2"
_TEST_LABEL_3 = "test_label_3"


class TestModel(InferenceModel):
    """
    Test model for development and testing purposes.
    Returns different labels based on content string.
    """

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        """
        Execute test inference - returns labels based on content string patterns.

        Expected data structure (Pydantic model):
        ModelInferenceRequestBatch(
            model_name="test-model",
            contents=[
                ModelInferenceRequest(
                    id="unique-identifier",
                    content="any string content"
                    # If content contains "return-label-1" -> returns label_1 with high score
                    # If content contains "return-label-2" -> returns label_2 with high score
                    # Otherwise -> returns equal scores for all labels
                )
            ]
        )

        Returns:
        ModelInferenceResultBatch(
            contents=[
                ModelInferenceResult(
                    id="unique-identifier",
                    label_evaluation=[
                        LabelEvaluation(label="test_label_1", score=0.944314),
                        LabelEvaluation(label="test_label_2", score=0.0334),
                        LabelEvaluation(label="test_label_3", score=0.0334)
                    ]
                )
            ]
        )
        """
        mapped_content = list(
            map(
                lambda element: ModelInferenceResult(
                    id=element.id, label_evaluation=self._get_label_evaluation(element)
                ),
                data.contents,
            )
        )

        return ModelInferenceResultBatch(contents=mapped_content)

    @staticmethod
    def _get_label_evaluation(element: ModelInferenceRequest):
        if "return-label-1" in element.content:
            return [
                LabelEvaluation(label=_TEST_LABEL_1, score=0.944314),
                LabelEvaluation(label=_TEST_LABEL_2, score=0.0334),
                LabelEvaluation(label=_TEST_LABEL_3, score=0.0334),
            ]

        elif "return-label-2" in element.content:
            return [
                LabelEvaluation(label=_TEST_LABEL_2, score=0.944314),
                LabelEvaluation(label=_TEST_LABEL_1, score=0.0334),
                LabelEvaluation(label=_TEST_LABEL_3, score=0.0334),
            ]

        return [
            LabelEvaluation(label=_TEST_LABEL_2, score=0.333),
            LabelEvaluation(label=_TEST_LABEL_1, score=0.333),
            LabelEvaluation(label=_TEST_LABEL_3, score=0.333),
        ]

    async def on_unload(self):
        pass

    async def on_load(self):
        pass
