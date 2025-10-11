FROM python:3.13
LABEL authors="martin"

RUN pip install poetry==2.2.1

ENV NVIDIA_VISIBLE_DEVICES=all \
    NVIDIA_DRIVER_CAPABILITIES=compute,utility
ENV POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_IN_PROJECT=0 \
    POETRY_VIRTUALENVS_CREATE=0 \
    POETRY_CACHE_DIR=/tmp/poetry_cache

ARG INFERENCE_SERVER_DIR=.

WORKDIR /usr/app
COPY $INFERENCE_SERVER_DIR/pyproject.toml $INFERENCE_SERVER_DIR/poetry.lock ./

RUN poetry install --no-root && rm -rf $POETRY_CACHE_DIR

COPY $INFERENCE_SERVER_DIR/src ./src/
COPY $INFERENCE_SERVER_DIR/scripts/ ./scripts/
RUN touch README.md

COPY ./proto ./proto
RUN chmod +x ./scripts/generate-server.sh
RUN ./scripts/generate-server.sh ./proto
RUN poetry install

ENTRYPOINT ["python", "src/inference_server/main.py"]