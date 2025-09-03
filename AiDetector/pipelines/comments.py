import logging, sys
from pathlib import Path

from comments.llm_labeling import label_dataset
from shared.deduplicate import JsonDeduplicator

logging.basicConfig(level=logging.INFO)

def process_comments(input_path: str):
    deduplicator = JsonDeduplicator()
    file_path = Path(input_path)

    deduplicated_file_path = deduplicator.deduplicate_dataset_file(file_path)

    if deduplicated_file_path is None:
        print("There was nothing to deduplicate!")

    label_dataset(deduplicated_file_path)


if __name__ == "__main__":
    # TODO: use argparse
    if len(sys.argv) != 2:
        print("Please provide path argument!")
    path_arg = sys.argv[1]
    process_comments(path_arg)