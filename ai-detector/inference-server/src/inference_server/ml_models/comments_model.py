from pathlib import Path
import json

import numpy as np
import logging

from onnxruntime import InferenceSession

from transformers import (
    AutoTokenizer,
)


class MLWorker:
    SUBFOLDER_NAME = "model"
    labels = {0: "clean", 1: "code_comment", 2: "technical_debt"}

    def __init__(self):
        subfolder = Path(MLWorker.SUBFOLDER_NAME)
        self.tokenizer = AutoTokenizer.from_pretrained(subfolder)
        self.session = InferenceSession(
            subfolder.joinpath("model.onnx"),
            providers=["CPUExecutionProvider", "CUDAExecutionProvider"],
        )

        self.__logger = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def map_json_content(request) -> str:
        content = request["content"]
        content = json.loads(content)

        return MLWorker.get_model_input(content)

    @staticmethod
    def get_model_input(request) -> str:
        return f"{request['commentType']}: {request['text']}"

    def execute(self, data):
        """
        Execute inference on comment data.

        Expected data structure:
        {
            "model_name": "comments-model",
            "contents": [
                {
                    "id": "unique-identifier",
                    "content": "{\"commentType\": \"JAVADOC\", \"text\": \"This is a comment\"}"
                    // Note: content is a JSON string that will be parsed
                }
            ]
        }

        After parsing content JSON string, it should contain:
        {
            "commentType": "string (e.g., 'JAVADOC', 'BLOCK', 'LINE')",
            "text": "the actual comment text"
        }

        Returns:
        {
            "contents": [
                {
                    "id": "unique-identifier",
                    "label_evaluation": [
                        {"label": "clean", "score": 0.95},
                        {"label": "code_comment", "score": 0.03},
                        {"label": "technical_debt", "score": 0.02}
                    ]
                }
            ]
        }
        """
        mapped = map(lambda request: self.map_json_content(request), data["contents"])

        inputs = self.tokenizer(
            list(mapped), return_tensors="np", padding=True, truncation=True
        )
        raw_results = self.session.run(None, {k: v for k, v in inputs.items()})

        inference_result = []
        logits = raw_results[0]
        probabilities = np.exp(logits) / np.exp(logits).sum(axis=1, keepdims=True)

        for index, prob in enumerate(probabilities):
            result_id = data["contents"][index]["id"]
            result = self._get_probabilities_per_label(prob)
            self.__logger.info("Inferred: id: '%s', labels: %s", result_id, result)

            inference_result.append({"id": result_id, "label_evaluation": result})

        return {"contents": inference_result}

    def _get_probabilities_per_label(self, probabilities):
        results = []
        for index, probability in enumerate(probabilities):
            results.append(
                {"label": self.labels[index], "score": float(round(probability, 10))}
            )

        return results

    def load(self):
        self.__logger.info("comments-model loaded!")

    def unload(self):
        self.__logger.info("comments-model loaded!")
