#!/bin/bash

GENERATED_SOURCES_PATH="src"
PROTO_PATH_FOLDER="${1:-../../common/proto}"
INFERENCE_PROTO_NAME="inference.proto"
INFERENCE_PROTO="${PROTO_PATH_FOLDER}/inference_server/proto/${INFERENCE_PROTO_NAME}"

poetry run python -m grpc_tools.protoc -I ${PROTO_PATH_FOLDER} --grpc_python_out=${GENERATED_SOURCES_PATH} --python_out=${GENERATED_SOURCES_PATH} --pyi_out=${GENERATED_SOURCES_PATH} ${INFERENCE_PROTO}