import unittest, os

from comments.static_labeling import label_code_snippets_comments, CODE_COMMENT_STATIC_ATTR
from shared import JSONDatasetList, save_dataset, load_dataset
from pathlib import Path

class StaticCodeLabelingTest(unittest.TestCase):
    TEST_FILE_NAME = "test-dataset.json"

    TEST_DATA = [
        {
            "text": "public static void String(String hello) { }",
            "expected_value": 1
        },
        {
            "text": "some random text String (String hello)",
            "expected_value": 0
        },
        {
            "text": "@return a list of events",
            "expected_value": 0
        },
        {
            "text": "{@link #getUser(String, String)}",
            "expected_value": 0
        },
        {
            "text": "{@see SystemUser#getUser(String, String)}",
            "expected_value": 0
        },
        {
            "text": "@see SystemUser#getUser(String, String)",
            "expected_value": 0
        },
        {
            "text": "a = b",
            "expected_value": 1
        },
        {
            "text": "@Bean",
            "expected_value": 1
        },
        {
            "text": "@bean",
            "expected_value": 0
        },
        {
            "text": "for (int i = 0; i < 2; i++)",
            "expected_value": 1
        },
        {
            "text": "package com.package.org;",
            "expected_value": 1
        },
        {
            "text": "\"com.package.org\"",
            "expected_value": 1
        },
        {
            "text": "Country name\n\ne.g. Czechia",
            "expected_value": 0
        }
    ]

    def tearDown(self):
        os.remove(self.TEST_FILE_NAME)

    def test_method_declaration(self):
        for index, data in enumerate(self.TEST_DATA):
            with self.subTest(i=index):
                self.perform_test(data["text"], data["expected_value"])

    def perform_test(self, tested_string: str, expected_value: int):
        dataset_path = self.create_dataset(tested_string)

        label_code_snippets_comments(dataset_path)

        dataset = load_dataset(dataset_path)
        self.assertTrue(len(dataset), 1)
        self.assertEqual(dataset[0][CODE_COMMENT_STATIC_ATTR], expected_value, f"Failed for test string '{tested_string}'")

    @staticmethod
    def create_dataset(text: str) -> Path:
        dataset: JSONDatasetList = [
            {
                "text": text
            }
        ]

        path = Path(StaticCodeLabelingTest.TEST_FILE_NAME)
        save_dataset(path, dataset)

        return path