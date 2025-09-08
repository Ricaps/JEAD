import re, logging
from pathlib import Path
from shared import load_dataset, save_dataset, StaticRuleLabeling

__LOGGER = logging.getLogger(__name__)

__UNFINISHED_LABEL = [
    re.compile(r"TODO|TO\s+DO", flags=re.IGNORECASE), re.compile(r"FIXME|\bFIX\b\s+ME", flags=re.IGNORECASE),
    re.compile(r"TBD", flags=re.IGNORECASE), re.compile(r"to\s+be\s+done", flags=re.IGNORECASE),
    re.compile(r"refactor", flags=re.IGNORECASE), re.compile(r"\btemp\b|temporary", flags=re.IGNORECASE),
    re.compile(r"\bbug\b", flags=re.IGNORECASE)]

__CODE_COMMENT_LABEL = [
    re.compile(r"TODO|TO\s+DO", flags=re.IGNORECASE)
]

UNFINISHED_COMMENT_STATIC_ATTR = "unfinished_comment_static"
CODE_COMMENT_STATIC_ATTR = "code_comment_static"


def label_unfinished_comments(dataset_path: Path):

    dataset = load_dataset(dataset_path)
    labeling = StaticRuleLabeling(positive_rules=__UNFINISHED_LABEL,
                                  labeled_attribute=UNFINISHED_COMMENT_STATIC_ATTR)

    dataset = labeling.label_dataset(dataset)
    save_dataset(dataset_path, dataset)


def label_code_snippets_comments(dataset_path: Path):
    dataset = load_dataset(dataset_path)
    labeling = StaticRuleLabeling(positive_rules=__CODE_COMMENT_LABEL,
                                  labeled_attribute=CODE_COMMENT_STATIC_ATTR)

    dataset = labeling.label_dataset(dataset)
    save_dataset(dataset_path, dataset)
