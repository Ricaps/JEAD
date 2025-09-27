#!/bin/bash

poetry run python -m grpc_tools.protoc -I ../../common/proto  --grpc_python_out=src/inference_server/proto --python_out=src/inference_server/proto ../../common/proto/inference.proto