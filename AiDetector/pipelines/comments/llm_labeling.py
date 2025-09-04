import asyncio
import logging
from pathlib import Path
from typing import Any

import numpy as np

from comments import prompts
from shared import JSONDatasetList, load_dataset, save_dataset, add_filename_suffix, batched_iterator
from shared.llm_connector import OllamaConnector, Model

UNFINISHED_COMMENT_ATTR = "unfinished_comment_llm"

__LOGGER = logging.getLogger(__name__)

async def __evaluate_unfinished_code(connector: OllamaConnector, element: dict[str, Any], index: int, part_number: int):
    __LOGGER.info(f"Evaluating element no. {index} of part: {part_number}")

    if UNFINISHED_COMMENT_ATTR in element:
        __LOGGER.info("Skipping")

    response = await connector.send(f"Here is the snippet: **{element["text"]}**")
    result = -1
    if "**no**" in response:
        result = 0
    elif "**yes**" in response:
        result = 1

    element[UNFINISHED_COMMENT_ATTR] = result

async def look_for_unfinished_code(dataset: JSONDatasetList, part_number: int) -> JSONDatasetList:
    connector = OllamaConnector(Model.LLAMA_3_1_8b)

    init_session_fnc = lambda : connector.init_session(prompts.__INIT_PROMPT_2)

    __LOGGER.info(f"Total number of elements: {len(dataset)}")

    try:
        await batched_iterator(10, dataset, init_session_fnc, lambda element, index : __evaluate_unfinished_code(connector, element, index, part_number))
    except KeyboardInterrupt:
        pass
    return dataset

async def label_dataset(path: Path) -> Path:
    """
    Performs labeling of the provided dataset

    :param path: path to dataset file
    :return: path to labeled dataset file
    """

    dataset: JSONDatasetList = load_dataset(path)
    number_of_parts = 20
    parts = np.array_split(dataset, number_of_parts)

    futures = [look_for_unfinished_code(parts[i].tolist(), i) for i in range(number_of_parts)]
    gathered_futures = await asyncio.gather(*futures)

    labeled_dataset = [item for sublist in gathered_futures for item in sublist]

    new_path = add_filename_suffix(path, "-todo-comment-llm-labeled.json")

    save_dataset(new_path, labeled_dataset)
