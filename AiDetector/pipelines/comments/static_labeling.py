import re, logging
from pathlib import Path
from shared import load_dataset, save_dataset, StaticRuleLabeling

__LOGGER = logging.getLogger(__name__)

__UNFINISHED_LABEL = [
    re.compile(r"TODO|TO\s+DO", flags=re.IGNORECASE), re.compile(r"FIXME|\bFIX\b\s+ME", flags=re.IGNORECASE),
    re.compile(r"TBD", flags=re.IGNORECASE), re.compile(r"to\s+be\s+done", flags=re.IGNORECASE),
    re.compile(r"refactor", flags=re.IGNORECASE), re.compile(r"\btemp\b|temporary", flags=re.IGNORECASE),
    re.compile(r"\bbug\b", flags=re.IGNORECASE)]

UNFINISHED_COMMENT_STATIC_ATTR = "unfinished_comment_static"


def label_unfinished_comments(dataset_path: Path):
    pattern_mapping = {
        1: __UNFINISHED_LABEL
    }

    dataset = load_dataset(dataset_path)
    labeling = StaticRuleLabeling(static_rules_mapping=pattern_mapping, not_found_label=0,
                                  labeled_attribute=UNFINISHED_COMMENT_STATIC_ATTR)

    dataset = labeling.label_dataset(dataset)
    save_dataset(dataset_path, dataset)
