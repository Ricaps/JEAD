from pathlib import Path

import numpy as np
from typing import Any
import logging
import json

from onnxruntime import InferenceSession, preload_dlls


class MLWorker:
    SUBFOLDER_NAME = "onnx"
    labels = {0: "clean", 1: "god_di"}

    def __init__(self):
        preload_dlls(directory="")
        subfolder = Path(MLWorker.SUBFOLDER_NAME)
        self.session = InferenceSession(
            subfolder.joinpath("model.onnx"),
            providers=["CPUExecutionProvider", "CUDAExecutionProvider"],
        )
        self.__logger = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def map_json_content(request):
        content_str = request["content"]
        content = json.loads(content_str)

        return [
            content["noom"],
            content["nooa"],
            content["nocm"],
            content["LCOM5"],
            content["cc"],
            content["loc"],
        ]

    def execute(self, data):
        """
        Execute inference on God class / Data class detection using code metrics.

        Expected data structure (dict representation of ModelInferenceRequestBatch):
        {
            "model_name": "god-di-model",
            "contents": [
                {
                    "id": "unique-identifier",
                    "content": '{"noom": 10, "nooa": 5, "nocm": 8, "LCOM5": 0.75, "cc": 15, "loc": 200}'
                }
            ]
        }

        The content field should be a JSON string containing these metrics:
        {
            "noom": int,  # Number of Methods
            "nooa": int,  # Number of Attributes
            "nocm": int,  # Number of Class Methods
            "LCOM5": float,  # Lack of Cohesion of Methods 5
            "cc": int,  # Cyclomatic Complexity
            "loc": int  # Lines of Code
        }

        Returns (dict representation of ModelInferenceResultBatch):
        {
            "contents": [
                {
                    "id": "unique-identifier",
                    "label_evaluation": [
                        {"label": "clean", "score": 0.0}
                        # OR
                        {"label": "god_di", "score": 1.0}
                    ]
                }
            ]
        }

        Note: The model performs binary classification. The score is either 0.0 (clean)
        or 1.0 (god_di), indicating the predicted class rather than a probability.
        """
        inputs = np.array(
            [MLWorker.map_json_content(content) for content in data["contents"]],
            dtype=np.float32,
        )

        raw_results = self.session.run(None, {"inputs": inputs})[0]

        model_inference_results = []
        for index, result in enumerate(raw_results):
            result_id = data["contents"][index]["id"]
            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, result)

            label_evaluation = {
                "label": MLWorker.labels[result],
                "score": float(result),
            }
            model_inference_results.append(
                {"id": result_id, "label_evaluation": [label_evaluation]}
            )

        return {"contents": model_inference_results}

    @staticmethod
    def _process_result(label: dict[str, Any]) -> dict[str, Any]:
        return {**label, "score": round(label["score"], 5)}

    def unload(self):
        print("God-DI model unloaded!")

    def load(self):
        print("God-DI model loaded!")
