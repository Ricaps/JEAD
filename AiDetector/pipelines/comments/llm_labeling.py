from pathlib import Path
from typing import Any

from comments import prompts
from shared import JSONDatasetList, load_dataset, save_dataset, change_file_name, batched_iterator
from shared.llm_connector import OllamaConnector, Model
import logging

UNFINISHED_COMMENT_ATTR = "unfinished_comment_llm"

__LOGGER = logging.getLogger(__name__)

def __evaluate_unfinished_code(connector: OllamaConnector, element: dict[str, Any], index: int):
    __LOGGER.info(f"Processing element no. {index}")

    if UNFINISHED_COMMENT_ATTR in element:
        __LOGGER.info(f"Skipping element no. {index}")

    response = connector.send(f"Here is the snippet: **{element["text"]}**")
    result = -1
    if "**no**" in response:
        result = 0
    elif "**yes**" in response:
        result = 1

    element[UNFINISHED_COMMENT_ATTR] = result

def look_for_unfinished_code(connector: OllamaConnector, dataset: JSONDatasetList) -> None:
    init_session_fnc = lambda : connector.init_session(prompts.__INIT_PROMPT_2)

    __LOGGER.info(f"Total number of elements: {len(dataset)}")

    try:
        batched_iterator(10, dataset, init_session_fnc, lambda element, index : __evaluate_unfinished_code(connector, element, index))
    except KeyboardInterrupt:
        pass

def label_dataset(path: Path) -> Path:
    """
    Performs labeling of the provided dataset

    :param path: path to dataset file
    :return: path to labeled dataset file
    """

    dataset: JSONDatasetList = load_dataset(path)

    connector = OllamaConnector(Model.LLAMA_3_1_8b)

    look_for_unfinished_code(connector, dataset)
    new_path = change_file_name(path, "dataset-llm-labeled.json")

    save_dataset(new_path, dataset)
