from pathlib import Path
from typing import Optional, Callable, Any
import json

import numpy as np
import logging

from onnxruntime import InferenceSession, preload_dlls
from sklearn.cluster import HDBSCAN
from transformers import AutoTokenizer
from umap import UMAP


class MLWorker:
    SUBFOLDER_NAME = "onnx"

    def __init__(self):
        preload_dlls(directory="")
        subfolder = Path(MLWorker.SUBFOLDER_NAME)
        self.tokenizer: Optional[Callable[[Any], Any]] = AutoTokenizer.from_pretrained(
            subfolder
        )
        self.session: Optional[InferenceSession] = InferenceSession(
            subfolder.joinpath("model.onnx"),
            providers=["CPUExecutionProvider", "CUDAExecutionProvider"],
        )
        self.__logger = logging.getLogger(self.__class__.__name__)

    def load(self):
        self.__logger.info("multi-service model loaded!")

    def unload(self):
        self.__logger.info("multi-service model unloaded!")

    def execute(self, data):
        """
        Execute inference on multi-service class data.

        Expected data structure:
        {
            "model_name": "multi-service-model",
            "contents": [
                {
                    "id": "unique-identifier",
                    "content": "{\"methods\": [{\"name\": \"methodName\", \"signature\": \"ReturnType methodName(ParamType param)\"}]}"
                    // Note: content is a JSON string that will be parsed
                }
            ]
        }

        After parsing content JSON string, it should contain:
        {
            "methods": [
                {
                    "name": "methodName",
                    "signature": "ReturnType methodName(ParamType param)"
                },
                // ... more methods
            ]
        }

        Returns:
        {
            "contents": [
                {
                    "id": "unique-identifier",
                    "label_evaluation": [
                        {"label": "clean", "score": 1.0}
                        // OR
                        {"label": "multi_service", "score": 1.0}
                    ]
                }
            ]
        }

        Logic: If class has < 10 methods OR methods cluster into <= 2 groups -> "clean"
               Otherwise -> "multi_service"
        """
        mapped_batch = list(
            map(lambda request: self.map_json_content(request), data["contents"])
        )

        results = []
        for identifier, methods in mapped_batch:
            if len(methods) < 10:
                results.append(
                    {
                        "id": identifier,
                        "label_evaluation": [{"label": "clean", "score": 1}],
                    }
                )
                continue

            inputs = self.tokenizer(
                methods,
                return_tensors="np",
                padding=True,
                truncation=True,
                max_length=512,
            )
            raw_results = self.session.run(None, {k: v for k, v in inputs.items()})

            embeddings = self._get_mean_pooled_embeddings(inputs, raw_results)
            embeddings = self._dimensional_reduction(embeddings)
            clusters_count = self._get_number_of_clusters(embeddings)

            if clusters_count <= 2:
                label_evaluation = {"label": "clean", "score": 1}
            else:
                label_evaluation = {"label": "multi_service", "score": 1}

            model_inference_result = {
                "id": identifier,
                "label_evaluation": [label_evaluation],
            }
            results.append(model_inference_result)

        model_inference_result_batch = {"contents": results}
        return model_inference_result_batch

    @staticmethod
    def _get_mean_pooled_embeddings(
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
    def map_json_content(request) -> tuple[str, list[str]]:
        content = request["content"]
        content = json.loads(content)

        return request["id"], MLWorker.get_model_input(content)

    @staticmethod
    def get_model_input(request) -> list[str]:
        return list(map(lambda method: method["signature"], request["methods"]))

    @staticmethod
    def _dimensional_reduction(embeddings: np.ndarray):
        n_samples = embeddings.shape[0]
        if n_samples <= 2:
            return embeddings

        n_neighbors = 5
        if n_samples <= n_neighbors:
            n_neighbors = n_samples - 1

        umap = UMAP(
            n_neighbors=n_neighbors,
            min_dist=0.0,
            n_components=15,
            metric="cosine",
            init="random",
        )
        return umap.fit_transform(embeddings)

    @staticmethod
    def _get_number_of_clusters(embeddings: np.ndarray) -> int:
        hdbscan = HDBSCAN(
            min_samples=1,
            min_cluster_size=3,
            metric="euclidean",
            allow_single_cluster=True,
            copy=False,
        )
        labels = hdbscan.fit_predict(embeddings)
        unique_labels = set(labels)
        labels_count = len(unique_labels) - (1 if -1 in unique_labels else 0)

        return labels_count
