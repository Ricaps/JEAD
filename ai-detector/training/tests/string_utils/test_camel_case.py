import unittest
from pipelines.shared.string_utils import split_camel_case


class TestCamelCase(unittest.TestCase):
    def test_two_words(self):
        result = split_camel_case("testMethod")
        self.assertEqual(2, len(result))
        self.assertEqual("test", result[0])
        self.assertEqual("method", result[1])

    def test_five_words(self):
        result = split_camel_case("testFirstSecondThirdFourthFifth")
        self.assertEqual(6, len(result))
        self.assertEqual("test", result[0])
        self.assertEqual("first", result[1])
        self.assertEqual("second", result[2])
        self.assertEqual("third", result[3])
        self.assertEqual("fourth", result[4])
        self.assertEqual("fifth", result[5])

    def test_consecutive_capitals(self):
        result = split_camel_case("testAB")
        self.assertEqual(2, len(result))
        self.assertEqual("test", result[0])
        self.assertEqual("ab", result[1])

    def test_consecutive_capitals_middle(self):
        result = split_camel_case("testATestSomething")
        self.assertEqual(4, len(result))
        self.assertEqual("test", result[0])
        self.assertEqual("a", result[1])
        self.assertEqual("test", result[2])
        self.assertEqual("something", result[3])

    def test_class_name(self):
        result = split_camel_case("TestClass")
        self.assertEqual(2, len(result))
        self.assertEqual("test", result[0])
        self.assertEqual("class", result[1])

    def test_one_word(self):
        result = split_camel_case("test")
        self.assertEqual(1, len(result))
        self.assertEqual("test", result[0])

    def test_empty_string(self):
        result = split_camel_case("")
        self.assertEqual(0, len(result))

    def test_underscore_skipped(self):
        result = split_camel_case("some_Test_Method")
        self.assertEqual(3, len(result))
        self.assertEqual("some", result[0])
        self.assertEqual("test", result[1])
        self.assertEqual("method", result[2])
