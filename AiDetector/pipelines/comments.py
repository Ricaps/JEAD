from pathlib import Path

from comments.llm_labeling import label_dataset
from shared.deduplicate import JsonDeduplicator
import logging

logging.basicConfig(level=logging.INFO)

def process_comments():
    deduplicator = JsonDeduplicator()
    file_path = Path("/media/martin/Big data1/datasets/pa165-git/output.json")

    deduplicated_file_path = deduplicator.deduplicate_dataset_file(file_path)

    if deduplicated_file_path is None:
        print("There was nothing to deduplicate!")

    label_dataset(deduplicated_file_path)


if __name__ == "__main__":
    process_comments()