from pipelines.shared import JSONDatasetList, input_until_integer
from typing import Any

class DifferencesEvaluator:

    def __init__(self, new_key_name: str, to_compare_keys: list[str], no_conflict_key: str):
        self.new_key_name = new_key_name
        self.to_compare_keys = to_compare_keys
        self.no_conflict_key = no_conflict_key

    @staticmethod
    def __print_value(object_key: str, value: str):
        print(f"Key: {object_key}, value: {value} ")

    def __values_are_equal(self, element: dict[str, Any]) -> bool:
        if len(self.to_compare_keys) == 0:
            return False

        values = [element[value] for value in self.to_compare_keys]
        return all(val == values[0] for val in values)

    def evaluate(self, dataset: JSONDatasetList):
        for index, element in enumerate(dataset):
            if self.new_key_name in element:
                continue

            if self.__values_are_equal(element):
                element[self.new_key_name] = element[self.no_conflict_key]
                continue

            print(f"Element {index} of {len(dataset)}: {element["text"]}")
            for key in self.to_compare_keys:
                self.__print_value(key, element[key])


            try:
                new_value = input_until_integer(f"Enter value for '{self.new_key_name}': ")
                element[self.new_key_name] = new_value
                print("-----")
            except KeyboardInterrupt:
                break