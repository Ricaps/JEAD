# Inference Server

This service exposes trained AI detection models over a gRPC API. It is responsible for:

- Loading one or more ML models from a configurable *models root* directory
- Managing an isolated Python virtual environment for the models
- Handling inference requests (batch inputs) and returning model predictions

It is designed to be embedded into the overall JEAD pipeline but can be run stand‑alone for development and testing.

---

## Docker image

A prebuilt image is published on Docker Hub:

- Docker Hub: https://hub.docker.com/repository/docker/ricaps/jead

For production-ready assemblies and versioned releases of this service, see:

- GitHub Releases: https://github.com/Ricaps/Jead/releases

The image is typically run via the provided Docker Compose files (see [Running with Docker Compose](#running-with-docker-compose)).

---

## Requirements

Local development requirements:

- **Python**: `>=3.13,<4.0` (as specified in `pyproject.toml`)
- **pip**: recent version that supports Python 3.13
- **Poetry**: `>=2.0` (project uses PEP 621 with `poetry-core`)
- **Docker** or **Podman**: to run the provided Docker Compose setups or containerized deployments
- **gRPC tooling** is installed via project dependencies (`grpcio`, `grpcio-tools`, etc.)

To install Poetry (if not already installed), see the official docs: https://python-poetry.org/docs/#installation

---

## Development setup

### 1. Install dependencies

From the `inference-server` directory:

```bash
# Ensure you are using Python 3.13 (e.g. via pyenv or your system Python)
python3 -m venv .venv
source .venv/bin/activate

# Install Poetry (if not already installed)
python -m pip install --upgrade pip
pip install "poetry>=2.0.0"

# Install project dependencies (main + dev)
poetry install --with dev
```

### 2. Generate gRPC server code

The gRPC server stubs need to be generated from the proto definitions before you can run the server. Still from the `inference-server` directory:

```bash
# Ensure the script is executable (only needed once)
chmod +x scripts/generate-server.sh

# Generate gRPC server code
scripts/generate-server.sh
```

This will compile the `.proto` files into Python gRPC code under `src/inference_server/proto`.

### 3. Configure server

Configuration is provided by environment variables read by `ServerConfig` (`src/inference_server/configuration/config.py`) via Pydantic settings. The main ones are:

- `ADDRESS` – gRPC listen address, e.g. `0.0.0.0`
- `PORT` – gRPC listen port, e.g. `8081`
- `MODELS_ROOT` – path to the models root directory (see [Models root structure](#models-root-structure))
- `MODELS_VENV_DIR_NAME` – name of the directory used for the models venv under the root (default: `.venv`)
- `USE_GPU` – `true`/`false`, whether to use GPU-specific requirements file
- `MODEL_COMMAND_TIMEOUT` – timeout in seconds for model commands (default: `120`)

You can define these in a local `.env` file in `inference-server/`:

```env
ADDRESS=0.0.0.0
PORT=8081
MODELS_ROOT=./tests/resources/model_root
MODELS_VENV_DIR_NAME=.venv
USE_GPU=false
MODEL_COMMAND_TIMEOUT=120
```

An example configuration file is provided as `.env.example` in this directory; you can copy it to `.env` and adjust values as needed.

### 4. Run the server with Poetry

Before running the server, make sure you have already:

- Installed dependencies with `poetry install --with dev`
- Generated the gRPC server code via `scripts/generate-server.sh`

From the `inference-server` directory:

```bash
poetry run python3 src/inference_server/main.py
```

This will:

1. Load configuration from environment / `.env`
2. Discover models in `MODELS_ROOT`
3. Start the gRPC server and register the Inference service

### 5. Run tests

There is a test suite validating the server, model storage, and basic integration behavior:

```bash
cd inference-server
poetry run pytest
```

---

## Running with Docker Compose

The repository includes Compose files to bring up the inference server via Docker:

- `compose.yaml` – production-like configuration that **pulls the image from Docker Hub** (`ricaps/jead`).
- `compose.local.yaml` – local development configuration that **builds the Docker image from this repository**.

Use them **separately**, depending on your use case:

### Use Docker Hub image (recommended to quickly try it out)

From the `inference-server` directory:

```bash
# Uses compose.yaml, pulls ricaps/jead from Docker Hub
docker compose -f compose.yaml up -d

# View logs
docker compose -f compose.yaml logs -f inference-server

# Stop
docker compose -f compose.yaml down
```

### Use local image built from source (for development)

From the `inference-server` directory:

```bash
# Uses compose.local.yaml, builds the image from the local Dockerfile
docker compose -f compose.local.yaml up -d

# View logs
docker compose -f compose.local.yaml logs -f inference-server

# Stop
docker compose -f compose.local.yaml down
```

Make sure to adjust the volume mounts and environment variables in the compose files so that `MODELS_ROOT` inside the container points to your model directory.

---

## Models root structure

The inference server loads models from the directory specified by `models_root` (`MODELS_ROOT` env var). At startup, `ModelStorage`:

1. Ensures a Python virtual environment exists under the models root (`models_venv_dir_name`, by default `.venv`).
2. Installs Python dependencies from a **single requirements file in the models root**.
3. Iterates over subdirectories in the models root.
4. For each subdirectory, checks for the presence of a model worker file.
5. Registers each valid model under its **folder name**.

The requirements file used for all models in that root is chosen based on `USE_GPU`:

- `USE_GPU=false` → `requirements.txt`
- `USE_GPU=true` → `requirements.gpu.txt`

Both of these live directly in the `models_root` directory and are shared by all contained models.

### Directory layout

A minimal layout looks like this (mirroring the test resources in `tests/resources/model_root`):

```text
models_root/
  .venv/                       # auto-created venv for all models in this root
  requirements.txt             # shared CPU dependencies for all models
  requirements.gpu.txt         # optional: shared GPU dependencies for all models
  existing-model/              # model name = "existing-model"
    worker.py                  # entrypoint for model; implements InferenceModel
  another-model/
    worker.py
```

Important details:

- The server **skips** any folders that:
  - Match `models_venv_dir_name` (e.g. `.venv`), or
  - Do not contain the required worker file (see `ModelWorkerManager.WORKER_FILE`).
- Model identification is based on the folder name (e.g. `existing-model`). Requests must use this name as `model_name`.
- A single virtual environment and a single requirements file (CPU or GPU variant) are used per models root; you do **not** define separate `requirements.txt` per model directory.

On first startup with a new models root, the server will:

1. Create the venv under `models_root/<MODELS_VENV_DIR_NAME>`.
2. Install `pip` into that venv.
3. Install packages from the selected root-level requirements file.

---

## Example: local model root for development

You can copy the sample structure from the tests directory as a starting point:

```bash
cd inference-server
cp -r tests/resources/model_root ./dev_model_root
```

Then point the server to this directory:

```env
# .env
MODELS_ROOT=./dev_model_root
```

Start the server (via Poetry or Docker) and issue gRPC requests using the model names corresponding to the subdirectory names (for example, `existing-model`).
