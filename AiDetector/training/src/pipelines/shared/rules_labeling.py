from pipelines.shared import JSONDatasetList
import re
from typing import AnyStr

RuleList = list[re.Pattern[AnyStr]]


class StaticRuleLabeling:
    def __init__(self, positive_rules: RuleList, labeled_attribute: str, positive_label: int = 1,
                 negative_label: int = 0,
                 negative_rules: RuleList = None):
        self.positive_rules = positive_rules
        self.negative_rules = negative_rules
        self.positive_label = positive_label
        self.negative_label = negative_label
        self.labeled_attribute = labeled_attribute

    @staticmethod
    def __check_regexes(value: str, rule_list: RuleList) -> bool:
        for regex in rule_list:
            if regex.search(value):
                return True

        return False

    def label_dataset(self, dataset: JSONDatasetList) -> JSONDatasetList:
        for element in dataset:
            new_label = self.negative_label
            element_value = element["text"]

            positive_result = self.__check_regexes(element_value, self.positive_rules)
            negative_result = False if self.negative_rules is None else self.__check_regexes(element_value, self.negative_rules)

            if positive_result and not negative_result:
                new_label = self.positive_label

            element[self.labeled_attribute] = new_label

        return dataset
