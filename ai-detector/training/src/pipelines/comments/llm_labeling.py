import asyncio
import logging
from pathlib import Path
from typing import Any, TypeVar, Callable, Generic, Type, Optional

import numpy as np

from pipelines.shared import (
    JSONDatasetList,
    load_dataset,
    save_dataset,
    add_filename_suffix,
    batched_iterator,
)
from pipelines.shared.llm_connector import OllamaConnector, Model
from pydantic import BaseModel

T = TypeVar("T", bound=BaseModel)


class LLMLabeler(Generic[T]):
    def __init__(
        self,
        init_prompt: str,
        run_prompt: str,
        labeled_attribute: str,
        model: Model,
        process_response: Callable[[Optional[T], dict[str, Any]], None],
        response_class: Type[T],
    ):
        """
        :param init_prompt: prompt send at the beginning / after reset of the session
        :param run_prompt: Prompt sent with each value
        :param labeled_attribute: attribute to the added to dataset with the labeling result
        """
        self.init_prompt = init_prompt
        self.run_prompt = run_prompt
        self.labeled_attribute = labeled_attribute
        self.model = model
        self.process_response = process_response
        self.response_class = response_class
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def __evaluate_element(
        self,
        connector: OllamaConnector[T],
        element: dict[str, Any],
        index: int,
        part_number: int,
    ):
        if index % 25 == 0:
            print(f"Evaluating element no. {index} of part: {part_number}")

        if self.labeled_attribute in element:
            self.__logger.info(f"Skipping element no. {index} of part: {part_number}")

        # response = await connector.send(f"{self.run_prompt}: **{element["text"]}**")
        response = await connector.send(element["text"])

        self.process_response(response, element)

    async def __label_dataset_part(
        self, dataset: JSONDatasetList, part_number: int
    ) -> JSONDatasetList:
        connector = OllamaConnector[T](self.model, self.response_class)

        def init_session_fnc():
            return connector.init_session(self.init_prompt)

        self.__logger.info(f"Total number of elements: {len(dataset)}")

        try:
            await batched_iterator(
                3,
                dataset,
                init_session_fnc,
                lambda element, index: self.__evaluate_element(
                    connector, element, index, part_number
                ),
            )
        except KeyboardInterrupt:
            pass

        return dataset

    async def label_dataset(self, path: Path, file_suffix: str) -> Path:
        """
        Performs labeling of the provided dataset

        :param path: path to dataset file
        :param file_suffix: suffix to be added to the file with results
        :return: path to labeled dataset filex
        """

        dataset: JSONDatasetList = load_dataset(path)
        number_of_parts = 20
        parts = np.array_split(dataset, number_of_parts)

        futures = [
            self.__label_dataset_part(parts[i].tolist(), i)
            for i in range(number_of_parts)
        ]
        gathered_futures = await asyncio.gather(*futures)

        labeled_dataset = [item for sublist in gathered_futures for item in sublist]

        new_path = add_filename_suffix(path, f"{file_suffix}.json")

        save_dataset(new_path, labeled_dataset)

        return new_path
