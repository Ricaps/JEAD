_TEST_LABEL_1 = "test_label_1"
_TEST_LABEL_2 = "test_label_2"
_TEST_LABEL_3 = "test_label_3"


class MLWorker:
    def load(self):
        print("Loaded")

    def unload(self):
        print("Unloaded")

    @staticmethod
    def _get_label_evaluation(element):
        if "return-label-1" in element["content"]:
            return [
                {"label": _TEST_LABEL_1, "score": 0.944314},
                {"label": _TEST_LABEL_2, "score": 0.0334},
                {"label": _TEST_LABEL_3, "score": 0.0334},
            ]

        elif "return-label-2" in element["content"]:
            return [
                {"label": _TEST_LABEL_2, "score": 0.944314},
                {"label": _TEST_LABEL_1, "score": 0.0334},
                {"label": _TEST_LABEL_3, "score": 0.0334},
            ]

        return [
            {"label": _TEST_LABEL_2, "score": 0.333},
            {"label": _TEST_LABEL_1, "score": 0.333},
            {"label": _TEST_LABEL_3, "score": 0.333},
        ]

    def execute(self, data):
        print(data)
        mapped_content = list(
            map(
                lambda element: {
                    "id": element["id"],
                    "label_evaluation": self._get_label_evaluation(element),
                },
                data["contents"],
            )
        )

        return {"contents": mapped_content}
