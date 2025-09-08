from shared import JSONDatasetList
import re
from typing import AnyStr


class StaticRuleLabeling:
    def __init__(self, static_rules_mapping: dict[int, list[re.Pattern[AnyStr]]], not_found_label: int, labeled_attribute: str):
        self.static_rules_mapping = static_rules_mapping
        self.labeled_attribute = labeled_attribute
        self.not_found_label = not_found_label

    def __check_regex(self, value: str) -> int:
        for label in self.static_rules_mapping.keys():
            for pattern in self.static_rules_mapping[label]:
                if pattern.search(value):
                    return label

        return self.not_found_label

    def label_dataset(self, dataset: JSONDatasetList) -> JSONDatasetList:
        for element in dataset:
            new_label = self.__check_regex(element["text"])
            element[self.labeled_attribute] = new_label

        return dataset
