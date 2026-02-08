import sys
from pathlib import Path
from importlib import util

WORKER_CLASS = "MLWorker"

LOAD_METHOD = "load"
UNLOAD_METHOD = "unload"
EXECUTE_METHOD = "execute"


def _load_worker_module(path: Path):
    script = util.spec_from_file_location("worker_module", path)
    module = util.module_from_spec(script)
    sys.modules["worker_module"] = module
    script.loader.exec_module(module)

    return module


def create_worker(path: Path):
    module = _load_worker_module(path)

    if not hasattr(module, WORKER_CLASS):
        raise NameError(f"The worker is missing class {WORKER_CLASS} at {path}")

    worker = module.MLWorker()
    for method_name in (LOAD_METHOD, UNLOAD_METHOD, EXECUTE_METHOD):
        if not hasattr(worker, method_name):
            raise NameError(
                f"The class {WORKER_CLASS} doesn't have method {method_name}"
            )

    worker.load()

    return worker


if __name__ == "__main__":
    created_worker = create_worker(
        Path("/media/martin/BigData/models/models_root/test-model/worker.py")
    )
