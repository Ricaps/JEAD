from pydantic import BaseModel


class ModelInferenceRequest(BaseModel):
    id: str
    content: str


class ModelInferenceRequestBatch(BaseModel):
    model_name: str
    contents: list[ModelInferenceRequest]


class ModelInferenceResult(BaseModel):
    id: str
    label: str


class ModelInferenceResultBatch(BaseModel):
    contents: list[ModelInferenceResult]
