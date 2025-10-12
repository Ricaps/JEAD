#!/bin/bash

CPU_OR_GPU="${1:-gpu}"

echo $CPU_OR_GPU
poetry install --no-root --with $CPU_OR_GPU