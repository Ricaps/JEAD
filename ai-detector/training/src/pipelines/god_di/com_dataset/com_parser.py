from pathlib import Path
import pandas as pd
from pandas import DataFrame


class ComDatasetParser:
    GLOBAL_PREFIX = "global"
    PCT_SUFFIX = "pct"

    def __init__(self, output_path: str, do_project_pct_calculation=False):
        self.output_path = output_path
        self.do_project_pct_calculation = do_project_pct_calculation

    def parse_com_dateset(self, path: str, wanted_metrics: list[str]):
        path = Path(path)
        for folder in path.rglob("*"):
            if not folder.is_dir():
                continue

            to_be_processed_files = {}
            for wanted_file in wanted_metrics:
                wanted_file_path = folder.joinpath(wanted_file + ".csv")
                if wanted_file_path.exists():
                    to_be_processed_files[wanted_file] = wanted_file_path

            if not all(key in to_be_processed_files.keys() for key in wanted_metrics):
                continue

            project_name = folder.name
            print(f"Processing folder {folder} (project name: {project_name})")
            self.process_folder(project_name, to_be_processed_files)

        full_df = pd.read_json(self.output_path, lines=True)
        full_df = full_df.drop_duplicates()
        self.calculate_global_pct(full_df, wanted_metrics)
        full_df.to_json(
            path_or_buf=self.output_path, mode="w", lines=True, orient="records"
        )

    def process_folder(self, project_name: str, files: dict[str, str]):
        data_frames: dict[str, DataFrame] = {}

        # Map all files to data frames
        for metric_name, file_path in files.items():
            data_frames[metric_name] = pd.read_csv(file_path)

        merged = None
        for metric_name, data_frame in data_frames.items():
            data_frame = data_frame.loc[data_frame[metric_name].notna()]
            if merged is None:
                merged = data_frame
            else:
                merged = merged.merge(data_frame, on="java_file", how="inner")

        if self.do_project_pct_calculation:
            self.calculate_project_pct(merged, list(data_frames.keys()))

        merged.insert(0, "project_name", project_name)
        merged.to_json(
            path_or_buf=self.output_path, mode="a", lines=True, orient="records"
        )

    @staticmethod
    def calculate_project_pct(data_frame: DataFrame, metrics: list[str]):
        for metric in metrics:
            metric_pct = data_frame[metric].rank(pct=True)
            data_frame[f"project_{metric}_pct"] = metric_pct

    @staticmethod
    def calculate_global_pct(data_frame: DataFrame, metrics: list[str]):
        for metric in metrics:
            metric_pct = data_frame[metric].rank(pct=True)
            column_name = ComDatasetParser.get_pct_column_name(
                ComDatasetParser.GLOBAL_PREFIX, metric
            )
            data_frame[column_name] = metric_pct

    @staticmethod
    def get_pct_column_name(prefix: str, metric: str):
        return f"{prefix}_{metric}_{ComDatasetParser.PCT_SUFFIX}"


if __name__ == "__main__":
    root_folder = "/media/martin/BigData/datasets/cam_2025/data/"
    output_path = "/media/martin/BigData/datasets/cam_2025/parsed.jsonl"
    parser = ComDatasetParser(output_path=output_path)
    parser.parse_com_dateset(root_folder, ["cc", "LCOM5", "nooa", "noom", "loc"])
