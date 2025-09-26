import asyncio
import json
import logging
from pathlib import Path

import numpy as np
from pydantic import BaseModel

from pipelines.shared import load_dataset, add_filename_suffix
from pipelines.shared.llm_connector import OpenAIConnector
from typing import Callable, Any

class ResponseModel(BaseModel):
    generated_snippets: list[str]

class LLMGenerator:

    def __init__(self, prompt: str, dataset_path: Path, file_suffix: str):
        self.connector = OpenAIConnector[ResponseModel](ResponseModel)
        self.prompt = prompt
        self.dataset_path = dataset_path
        self.file_suffix = file_suffix
        self.__logger = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def __save_content(path: Path, content: dict[str, Any]):
        if path.exists():
            with open(path, "r") as file:
                try:
                    file_content = json.load(file)
                except json.JSONDecodeError:
                    file_content = []

            file_content.append(content)
            with open(path, "w") as file:
                json.dump(file_content, file, indent=2)
        else:
            with open(path, "w+") as file:
                file.write(json.dumps([content], indent=2))


    async def for_each_element(self, filter_fnc: Callable[[dict[str, Any]], bool]):
        dataset = load_dataset(self.dataset_path)
        filtered_dataset = list(filter(filter_fnc, dataset))

        number_of_parts = 5
        parts = np.array_split(filtered_dataset, number_of_parts)

        futures = [self.process_part(index, part) for index, part in enumerate(parts)]
        await asyncio.gather(*futures)

    async def process_part(self, part_number: int, dataset):
        new_file_path = add_filename_suffix(self.dataset_path, f"-{str(part_number)}-{self.file_suffix}")
        generated = []

        for index, element in enumerate(dataset):
            self.__logger.info(f"Generating message for element {index} of {len(dataset)}")

            input_text = element["text"]
            response = await self.connector.send(self.prompt, input_text)

            generated_snippets = response.generated_snippets
            # generated_snippets = ["a", "b", "c", str(part_number)]
            if len(generated_snippets) == 0:
                self.__logger.warning(f"There was no generated snippet for {input_text}")
            generated.extend(generated_snippets)

            content = {
                "origin": input_text,
                "generated": generated
            }
            self.__save_content(new_file_path, content)
            generated.clear()
