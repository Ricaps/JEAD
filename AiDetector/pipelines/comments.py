from pathlib import Path
from shared.deduplicate import JsonDeduplicator

deduplicator = JsonDeduplicator()
file_path = Path("/media/martin/Big data1/datasets/pa165-git/output.json")

deduplicator.deduplicate_dataset_file(file_path)