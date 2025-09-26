import json
from pathlib import Path
from typing import Any, Callable
from pipelines.shared.utils import load_dataset, input_until_integer, save_dataset


class ManualLabeler:
    def __init__(self, include_fnc: Callable[[dict[str, Any]], bool], prop_key: str):
        self.include_fnc = include_fnc
        self.prop_key = prop_key

    def evaluate_labeled(self, path: Path):
        dataset = load_dataset(path)

        print(f"Dataset length: {len(dataset)}")
        start_number = input_until_integer("Enter start number: ")
        for index, element in enumerate(dataset):
            if index < start_number:
                continue
            try:
                self.__evaluate_element(index, element)
            except KeyboardInterrupt:
                break

        save_dataset(path, dataset)

    def __evaluate_element(self, index: int, element: dict[str, Any]):
        if self.include_fnc(element):
            print(f"Element {index}")
            print(json.dumps(element, indent=2))
            new_value = input_until_integer("Insert value: ")
            element[self.prop_key] = new_value
            print("-------")
