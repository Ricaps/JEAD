from datasets import Dataset
import random


def random_undersample(
    dataset: Dataset, undersampled_label: str, target_length: int
) -> Dataset:
    seed = 123

    random.seed(seed)
    shuffled_indexes = list(range(len(dataset)))
    random.shuffle(shuffled_indexes)

    indexes = []
    current_length = 0

    for index in shuffled_indexes:
        element = dataset[index]
        labels_arr = element["labels"]

        if undersampled_label not in labels_arr:
            indexes.append(index)
            continue

        if (
            len(labels_arr) == 1
            and undersampled_label in labels_arr
            and current_length < target_length
        ):
            indexes.append(index)
            current_length += 1
            continue

    return dataset.select(indexes)
