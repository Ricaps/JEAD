import json
from pathlib import Path
from typing import Iterable, Any, Callable, TypeVar, Coroutine

from shared.types import JSONDatasetList


def load_dataset(path: Path) -> JSONDatasetList:
    dataset: JSONDatasetList = []

    with open(path, "r") as file:
        dataset = json.load(file)

    return dataset

def save_dataset(path: Path, dataset: JSONDatasetList) -> None:
    with open(path, "w") as file:
        file.write(json.dumps(dataset, indent=2))

def add_filename_suffix(path: Path, new_name: str) -> Path:
    parent_folder = path.parents[0]
    file_name = path.name.split(".")[0]
    output_file_name = file_name + new_name

    return parent_folder.joinpath(Path(output_file_name))


T = TypeVar('T')
async def batched_iterator(call_iterations: int, items: Iterable[T], call_fnc: Callable[[], Coroutine[Any, Any, Any]], for_each_fnc: Callable[[T, int], Coroutine[Any, Any, None]]) -> None:
    """
    Iterates given items. After call_iterations number is reached, calls the call_fnc.
    call_fnc is called also before first iteration
    :param call_iterations: number of iterations when the call_fnc is called
    :param items: items to be iterated
    :param for_each_fnc: function called in each iteration
    :param call_fnc: function to be called at the start or each time the call_iterations number is reached
    :return: None
    """
    current_iteration = 0
    await call_fnc()

    for index, item in enumerate(items):
        await for_each_fnc(item, index)
        current_iteration += 1

        if current_iteration == call_iterations:
            await call_fnc()
            current_iteration = 0


def input_until_integer(input_str: str) -> int:
    while True:
        value = input(input_str).strip()
        if value.startswith("+"):
            if value[1:].isdigit(): return int(value)
        elif value.startswith("-"):
            if value[1:].isdigit(): return -int(value)
        elif value.isdigit():
            return int(value)

