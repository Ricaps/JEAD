import json
from pathlib import Path
from typing import Any
from shared.utils import load_dataset, input_until_integer, save_dataset

class ManualLabeler:

    def __init__(self, path: Path):
        self.path = path


    def evaluate_labeled(self, prop_key: str, value: int):
        dataset = load_dataset(self.path)

        print(f"Dataset length: {len(dataset)}")
        for index, element in enumerate(dataset):
            try:
                self.__evaluate_element(index, element, prop_key, value)
            except KeyboardInterrupt:
                break

        save_dataset(self.path, dataset)

    @staticmethod
    def __evaluate_element(index: int, element: dict[str, Any], prop_key: str, value: int):
        if prop_key in element and element[prop_key] == value:
            print(f"Element {index}")
            print(json.dumps(element, indent=2))
            new_value = input_until_integer("Insert value: ")
            element[prop_key] = new_value
            print("-------")