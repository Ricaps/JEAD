import asyncio
from typing import Optional, Callable, Any

import numpy as np
from aiopath import AsyncPath
import logging

from onnxruntime import InferenceSession
from pydantic import BaseModel
from sklearn.cluster import HDBSCAN
from transformers import AutoTokenizer
from umap import UMAP

from inference_server.ml_models.inference_model import InferenceModel
from inference_server.model.inference_model import (
    ModelInferenceRequestBatch,
    ModelInferenceResultBatch,
    ModelInferenceRequest,
    ModelInferenceResult,
    LabelEvaluation,
)
from inference_server.model.validation import validate_model_and_get
from inference_server.util.onnx_util import load_onnx


class InputMethod(BaseModel):
    name: str
    signature: str


class InputRequest(BaseModel):
    methods: list[InputMethod]


class MultiServiceModel(InferenceModel):
    SUBFOLDER_NAME = "onnx"

    def __init__(self, model_root_path: AsyncPath):
        super().__init__(model_root_path)
        self.tokenizer: Optional[Callable[[Any], Any]] = None
        self.session: Optional[InferenceSession] = None
        self.umap: Optional[UMAP] = None
        self.hdbscan: Optional[HDBSCAN] = None
        self._access_lock = asyncio.Lock()
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def on_load(self):
        path = self._model_root_path.joinpath(MultiServiceModel.SUBFOLDER_NAME)
        async with self._access_lock:
            self.tokenizer = AutoTokenizer.from_pretrained(path)
            self.session = load_onnx(path.joinpath("model.onnx"))
            self.umap = UMAP(
                n_neighbors=5,
                min_dist=0.0,
                n_components=15,
                metric="euclidean",
                random_state=42,
                init="random",
            )
            self.hdbscan = HDBSCAN(
                min_samples=1,
                min_cluster_size=3,
                metric="euclidean",
                allow_single_cluster=True,
            )

    async def on_unload(self):
        async with self._access_lock:
            self.tokenizer = None
            self.session = None
            self.umap = None
            self.hdbscan = None

    async def execute(
        self, data: ModelInferenceRequestBatch
    ) -> Optional[ModelInferenceResultBatch]:
        mapped_batch = list(
            map(lambda request: self.map_json_content(request), data.contents)
        )

        results: list[ModelInferenceResult] = []
        for identifier, methods in mapped_batch:
            async with self._access_lock:
                inputs = self.tokenizer(
                    methods,
                    return_tensors="np",
                    padding=True,
                    truncation=True,
                    max_length=512,
                )
                raw_results = self.session.run(None, {k: v for k, v in inputs.items()})
                embeddings = await self._get_mean_pooled_embeddings(inputs, raw_results)
                embeddings = self._normalize_embeddings(embeddings)
                embeddings = self._dimensional_reduction(embeddings)
                clusters_count = self._get_number_of_clusters(embeddings)

            if clusters_count <= 2:
                label_evaluation = LabelEvaluation(label="clean", score=1)
            else:
                label_evaluation = LabelEvaluation(label="multi_service", score=1)

            results.append(
                ModelInferenceResult(id=identifier, label_evaluation=[label_evaluation])
            )

        return ModelInferenceResultBatch(contents=results)

    @staticmethod
    def _normalize_embeddings(embeddings) -> np.ndarray:
        l2_normalized = np.linalg.norm(embeddings, ord=2, axis=1, keepdims=True)
        return embeddings / np.maximum(l2_normalized, 1e-12)

    @staticmethod
    async def _get_mean_pooled_embeddings(
        inputs: dict[str, np.ndarray], raw_results: list[np.ndarray]
    ) -> np.ndarray:
        last_hidden_state = raw_results[0]
        attention_mask = inputs["attention_mask"]
        expanded_mask = attention_mask[..., np.newaxis]
        embeddings = last_hidden_state * expanded_mask
        embeddings_sum = embeddings.sum(axis=1)
        token_count = expanded_mask.sum(axis=1)
        return embeddings_sum / token_count

    @staticmethod
    def map_json_content(request: ModelInferenceRequest) -> tuple[str, list[str]]:
        content = request.content
        input_request = validate_model_and_get(content, InputRequest)

        return request.id, MultiServiceModel.get_model_input(input_request)

    @staticmethod
    def get_model_input(request: InputRequest) -> list[str]:
        return list(map(lambda method: method.signature, request.methods))

    def _dimensional_reduction(self, embeddings: np.ndarray):
        return self.umap.fit_transform(embeddings)

    def _get_number_of_clusters(self, embeddings: np.ndarray) -> int:
        labels = self.hdbscan.fit_predict(embeddings)
        unique_labels = set(labels)
        labels_count = len(unique_labels) - (1 if -1 in unique_labels else 0)

        return labels_count
