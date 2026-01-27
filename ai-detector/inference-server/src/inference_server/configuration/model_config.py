from typing import Dict
from pathlib import Path
import yaml

from pydantic.v1 import BaseModel


class Model(BaseModel):
    host: str
    port: int


class ModelsConfig(BaseModel):
    models: Dict[str, Model]

    @classmethod
    def from_yaml(cls, path: Path) -> "ModelsConfig":
        with open(path, "r") as f:
            raw_data = yaml.safe_load(f)
        return cls(models=raw_data)
