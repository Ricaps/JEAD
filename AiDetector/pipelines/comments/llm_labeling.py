import asyncio
import logging
from pathlib import Path
from typing import Any

import numpy as np

from shared import JSONDatasetList, load_dataset, save_dataset, add_filename_suffix, batched_iterator
from shared.llm_connector import OllamaConnector, Model


class LLMLabeler:

    def __init__(self, init_prompt: str, run_prompt: str, labeled_attribute: str, response_mapping: dict[str, int], model: Model):
        """
        :param init_prompt: prompt send at the beginning / after reset of the session
        :param run_prompt: Prompt sent with each value
        :param labeled_attribute: attribute to the added to dataset with the labeling result
        :param response_mapping: mapping of possible responses to the result
        """
        self.init_prompt = init_prompt
        self.run_prompt = run_prompt
        self.labeled_attribute = labeled_attribute
        self.response_mapping = response_mapping
        self.model = model
        self.__logger = logging.getLogger(self.__class__.__name__)

    async def __evaluate_element(self, connector: OllamaConnector, element: dict[str, Any], index: int,
                                 part_number: int):
        self.__logger.info(f"Evaluating element no. {index} of part: {part_number}")

        if self.labeled_attribute in element:
            self.__logger.info(f"Skipping element no. {index} of part: {part_number}")

        response = await connector.send(f"{self.run_prompt}: **{element["text"]}**")

        matching_key = [key for key in self.response_mapping.keys() if key in response.lower()]

        result = -1
        if len(matching_key) == 1:
            result = self.response_mapping[matching_key[0]]
        else:
            self.__logger.warning(
                f"Response for element no. {index} of part: {part_number} matches unexpected number of possible responses!")

        element[self.labeled_attribute] = result

    async def __label_dataset_part(self, dataset: JSONDatasetList, part_number: int) -> JSONDatasetList:
        connector = OllamaConnector(self.model)

        init_session_fnc = lambda: connector.init_session(self.init_prompt)

        self.__logger.info(f"Total number of elements: {len(dataset)}")

        try:
            await batched_iterator(10, dataset, init_session_fnc,
                                   lambda element, index: self.__evaluate_element(connector, element, index,
                                                                                  part_number))
        except KeyboardInterrupt:
            pass

        return dataset

    async def label_dataset(self, path: Path, file_suffix: str) -> Path:
        """
        Performs labeling of the provided dataset

        :param path: path to dataset file
        :param file_suffix: suffix to be added to the file with results
        :return: path to labeled dataset file
        """

        dataset: JSONDatasetList = load_dataset(path)
        number_of_parts = 20
        parts = np.array_split(dataset, number_of_parts)

        futures = [self.__label_dataset_part(parts[i].tolist(), i) for i in range(number_of_parts)]
        gathered_futures = await asyncio.gather(*futures)

        labeled_dataset = [item for sublist in gathered_futures for item in sublist]

        new_path = add_filename_suffix(path, f"{file_suffix}.json")

        save_dataset(new_path, labeled_dataset)

        return new_path
