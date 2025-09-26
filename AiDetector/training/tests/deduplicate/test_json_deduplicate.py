import unittest

from pipelines.shared import deduplicate


class JsonDeduplicateTest(unittest.TestCase):

    def setUp(self):
        self.deduplicator = deduplicate.JsonDeduplicator()

    def test_json_deduplication_deduplicated_correctly(self):
        list_dict = [
            {
                "commentType": "LINE",
                "text": "test"
            },
            {
                "commentType": "LINE",
                "text": "test"
            }
        ]

        deduplicated = self.deduplicator.deduplicate_dataset(list_dict)
        self.assertListEqual([list_dict[0]], deduplicated, "Json was not deduplicated correctly")
        self.assertEqual(len(deduplicated), 1, "Deduplicated array has unexpected size")


    def test_json_deduplication_all_keys(self):
        list_dict = (
            [{
                "commentType": "LINE",
                "text": "test"
            },
                {
                    "commentType": "BLOCK",
                    "text": "test"
                }],
            [{
                "commentType": "LINE",
                "text": "test"
            },
                {
                    "commentType": "LINE",
                    "text": "test2"
                }],
        )

        for index, element in enumerate(list_dict):
            with self.subTest(i=index):
                deduplicated = self.deduplicator.deduplicate_dataset(element)
                self.assertListEqual(element, deduplicated, "Json was deduplicated even if keys are different")
                self.assertEqual(len(deduplicated), 2, "Array was deduplicated even it contains different keys")
