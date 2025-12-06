from enum import Enum

import pandas as pd
from pandas import DataFrame
from pydantic.dataclasses import dataclass

from pipelines.god_di.com_dataset.com_parser import ComDatasetParser


class ComparisonDirection(str, Enum):
    lower_than = "<"
    greater_than = ">"


@dataclass
class DirectLabelValueMapping:
    metric: str
    value: float
    direction: ComparisonDirection
    label: int


class ComDatasetLabeler:
    LABEL_COLUMN_NAME = "label"

    def __init__(
        self,
        metric_percentile_mapping: dict[str, float],
        concrete_value_mapping: list[DirectLabelValueMapping],
    ):
        self.metric_percentile_mapping: dict[str, float] = metric_percentile_mapping
        self.concrete_value_mapping: list[DirectLabelValueMapping] = (
            concrete_value_mapping
        )

    def do_labeling(self, data_frame: DataFrame):
        self._label_metric_percentile(data_frame)
        self._label_using_label_value_mapping(data_frame)
        self._set_non_to_zero(data_frame)

        data_frame[self.LABEL_COLUMN_NAME] = data_frame[self.LABEL_COLUMN_NAME].astype(
            "Int64"
        )

    def _label_metric_percentile(self, data_frame) -> None:
        """
        Do labeling of using percentile values for each defined metric column.\n
        If some value exceeds defined percentile in the dict mapping, positive label is automatically applied \n
        Directly mutates the input data_frame
        Args:
            data_frame: input data

        Returns: None

        """
        for metric, percentile in self.metric_percentile_mapping.items():
            column_name = ComDatasetParser.get_pct_column_name(
                ComDatasetParser.GLOBAL_PREFIX, metric
            )
            data_frame.loc[
                data_frame[column_name] > percentile, self.LABEL_COLUMN_NAME
            ] = 1

    def _label_using_label_value_mapping(self, data_frame):
        for mapping in self.concrete_value_mapping:
            query = data_frame.query(
                f"{mapping.metric} {mapping.direction.value}= {mapping.value}"
            )
            data_frame.loc[query.index, self.LABEL_COLUMN_NAME] = mapping.label

    def _set_non_to_zero(self, data_frame):
        data_frame.loc[
            data_frame[self.LABEL_COLUMN_NAME].isna(), self.LABEL_COLUMN_NAME
        ] = 0


def undersample(
    data_frame: DataFrame, frac: float, label: str, value: int
) -> DataFrame:
    data_frame_equals = data_frame.loc[data_frame[label] == value]
    data_frame_nequals = data_frame.loc[data_frame[label] != value]

    data_frame_equals = data_frame_equals.sample(frac=frac)
    return pd.concat([data_frame_equals, data_frame_nequals])
