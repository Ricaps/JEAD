# JEAD
JEAD is application for detection of anti-patterns with focus especially on enterprise specific anti-patterns.

Assembly releases: https://github.com/Ricaps/Jead/releases

## Requirements
- Java 25
- Docker

## Project structure
- `detector/`: Java detector responsible for anti-pattern detection.
- `ai-detector/`: Python tooling, training, inference server, and GitHub mining utilities.
  - `ai-detector/inference-server/`: gRPC/HTTP inference server used by the detector.
  - `ai-detector/training/`: Training pipelines and experiments for model development.
  - `ai-detector/github-miner/`: Data collection utilities.
- `test-fixtures/`: Sample projects with intentional anti-patterns used for tests.
- `assembly/`: Assembly packaging for distributing Jead.
- `build-plugins/`: Maven/Gradle plugins and shared build tooling.
- `common/`: Shared protobuf definitions.
- `experiments/`: Local experiments and datasets.

## Local development
1) Start the inference server
- See the inference server README: `ai-detector/inference-server/README.md`
2) Start and run the detector
- See the detector README: `detector/README.md`

## Build from repository root
Build JEAD using following command:
```shell
mvn package
```

For more information on how to use JEAD please see [README of the detector module](./detector/README.md).
