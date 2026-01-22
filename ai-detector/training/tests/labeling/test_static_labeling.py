import unittest
import re

from pipelines.shared import StaticRuleLabeling
from pipelines.shared import JSONDatasetList

LABELED_ATTRIBUTE = "test_attribute"


class StaticLabelingTest(unittest.TestCase):
    TODO_REGEX = re.compile(r"TODO|TO\s+DO", flags=re.IGNORECASE)
    PLAYER_SERVICE_REGEX = re.compile(r"\bplayerService\b", flags=re.IGNORECASE)
    NOT_MATCHING_REGEX = re.compile(r"\bfoo\b", flags=re.IGNORECASE)
    DATASET: JSONDatasetList = [
        {
            "text": "TODO playerService not being mocked",
        }
    ]

    def test_positive_regexes_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE, positive_rules=[self.TODO_REGEX]
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 1)

    def test_positive_matches_negative_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE,
            positive_rules=[self.TODO_REGEX],
            negative_rules=[self.PLAYER_SERVICE_REGEX],
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 0)

    def test_positive_matches_negative_not_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE,
            positive_rules=[self.TODO_REGEX],
            negative_rules=[self.NOT_MATCHING_REGEX],
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 1)

    def test_positive_not_matches_negative_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE,
            positive_rules=[self.NOT_MATCHING_REGEX],
            negative_rules=[self.TODO_REGEX],
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 0)

    def test_positive_not_matches_negative_not_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE,
            positive_rules=[self.NOT_MATCHING_REGEX],
            negative_rules=[self.NOT_MATCHING_REGEX],
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 0)

    def test_multiple_regexes_matches(self):
        labeler = StaticRuleLabeling(
            labeled_attribute=LABELED_ATTRIBUTE,
            positive_rules=[self.PLAYER_SERVICE_REGEX, self.TODO_REGEX],
        )
        dataset = labeler.label_dataset(self.DATASET)

        self.assertEqual(len(dataset), 1)
        self.assertEqual(dataset[0][LABELED_ATTRIBUTE], 1)
