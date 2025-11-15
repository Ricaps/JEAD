from pathlib import Path
import pandas as pd
from pandas import DataFrame


class ComDatasetParser:
    def __init__(self, output_path: str):
        self.output_path = output_path

    def parse_com_dateset(self, path: str, wanted_files: list[str]):
        path = Path(path)
        for folder in path.rglob("*"):
            if not folder.is_dir():
                continue

            to_be_processed_files = {}
            for wanted_file in wanted_files:
                wanted_file_path = folder.joinpath(wanted_file + ".csv")
                if wanted_file_path.exists():
                    to_be_processed_files[wanted_file] = wanted_file_path

            if not all(key in to_be_processed_files.keys() for key in wanted_files):
                continue

            print(f"Processing folder {folder}")
            self.process_folder(to_be_processed_files)

        full_df = pd.read_json(self.output_path, lines=True)
        full_df = full_df.drop_duplicates()
        full_df.to_json(
            path_or_buf=self.output_path, mode="w", lines=True, orient="records"
        )

    def process_folder(self, files: dict[str, str]):
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

        merged.to_json(
            path_or_buf=self.output_path, mode="a", lines=True, orient="records"
        )


if __name__ == "__main__":
    root_folder = "/media/martin/BigData/datasets/cam_2025/data/"
    output_path = "/media/martin/BigData/datasets/cam_2025/parsed.jsonl"
    parser = ComDatasetParser(output_path=output_path)
    parser.parse_com_dateset(root_folder, ["cc", "LCOM5", "nooa", "noom", "loc"])
