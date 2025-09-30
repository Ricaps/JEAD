from pydantic import BaseModel


class ModelInferenceRequest(BaseModel):
    id: str
    content: str


class ModelInferenceRequestBatch(BaseModel):
    model_name: str
    contents: list[ModelInferenceRequest]


class LabelEvaluation(BaseModel):
    label: str
    score: float


class ModelInferenceResult(BaseModel):
    id: str
    label_evaluation: list[LabelEvaluation]


class ModelInferenceResultBatch(BaseModel):
    contents: list[ModelInferenceResult]
