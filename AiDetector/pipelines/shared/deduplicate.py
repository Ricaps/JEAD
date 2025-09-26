import json
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Generic, TypeVar

from pipelines.shared import JSONDatasetList
from pipelines.shared import add_filename_suffix, save_dataset
from pipelines.shared.exception import PathNotFileException

T = TypeVar('T')
class DatasetDeduplicator(ABC, Generic[T]):

    @abstractmethod
    def deduplicate_dataset(self, dataset: T):
        pass

    @abstractmethod
    def deduplicate_dataset_file(self, path: Path):
        pass

class JsonDeduplicator(DatasetDeduplicator[JSONDatasetList]):
    _OUTPUT_FILE_SUFFIX = "-deduplicated.json"

    def deduplicate_dataset_file(self, path: Path) -> Path | None:
        """
        Deduplicates provided file with JSON-array like content
        :param path: Path to the input file
        :return: Path: Path to the output file
        """
        if path.is_dir():
            raise PathNotFileException("Specified path is not file")

        deduplicated = None
        with open(path, "r") as file:
            json_dataset = json.load(file)
            deduplicated = self.deduplicate_dataset(json_dataset)

        if deduplicated is None:
            return None

        output_path = add_filename_suffix(path, self._OUTPUT_FILE_SUFFIX)

        save_dataset(output_path, deduplicated)

        return output_path


    def deduplicate_dataset(self, dataset: JSONDatasetList):
        unique_items: JSONDatasetList = list()
        dataset_set = set()
        for element in dataset:
            hashable_element = tuple(sorted(element.items()))
            if hashable_element in dataset_set:
                continue
            dataset_set.add(hashable_element)
            unique_items.append(element)

        return unique_items



