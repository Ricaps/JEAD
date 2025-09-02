from typing import Any, Generic, TypeVar
from pathlib import Path
from abc import ABC, abstractmethod
from .exception import PathNotFileException
import json

JSONDatasetList = list[dict[str, Any]]

T = TypeVar('T')
class DatasetDeduplicator(ABC, Generic[T]):

    @abstractmethod
    def deduplicate_dataset(self, dataset: T):
        pass

    @abstractmethod
    def deduplicate_dataset_file(self, path: Path):
        pass

class JsonDeduplicator(DatasetDeduplicator[JSONDatasetList]):

    def deduplicate_dataset_file(self, path: Path) -> None:
        """
        :param path: Path to the input file
        :raises: PathNotFileException when provided path is not file
        :return:
        """
        if path.is_dir():
            raise PathNotFileException("Specified path is not file")

        deduplicated = None
        with open(path, "r") as file:
            json_dataset = json.load(file)
            deduplicated = self.deduplicate_dataset(json_dataset)

        if deduplicated is None:
            return

        parent_folder = path.parents[0]
        file_name = path.name.split(".")[0]
        output_file_name = file_name + "-deduplicated.json"
        output_path = parent_folder.joinpath(Path(output_file_name))

        with open(output_path, "w+") as output_file:
            output_file.write(json.dumps(deduplicated, indent = 3))


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



