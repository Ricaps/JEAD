# JEAD - Java AI Detector

This is the JEAD (Java AI Detector) distribution package. It contains everything you need to run the AI-powered code analysis tool.

## Prerequisites

Before running JEAD, ensure you have the following installed:

### Required Dependencies

1. **Java 25**
   - JEAD requires Java Development Kit (JDK) 25 or higher
   - Check your Java version:
     ```bash
     java -version
     ```
   - Download from: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)

2. **Docker**
   - Required for running the AI inference server
   - Check if Docker is installed:
     ```bash
     docker --version
     ```
   - Download from: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

3. **Docker Compose**
   - Required for orchestrating the inference server container
   - Check if Docker Compose is installed:
     ```bash
     docker compose version
     ```
   - Usually included with Docker Desktop, or install separately: [Docker Compose](https://docs.docker.com/compose/install/)

## Package Contents

This distribution contains:

- `detector.jar` - The main JEAD detector application (Spring Boot executable JAR)
- `compose.yaml` - Docker Compose configuration for the AI inference server
- `config/application.yml` - Configuration file for the detector
- `.env` - Environment configuration file (edit to set your models path)
- `README.md` - This file

## Setup

### 1. Configure Models Path

Before starting, you need to specify where AI models are stored on your host machine. Edit the `.env` file and update the path:

```bash
MODELS_ROOT_HOST=/path/to/your/models
```

Replace `/path/to/your/models` with the absolute path to your models directory on your host machine.

### 2. Start the AI Inference Server

The detector requires the inference server to be running. Start it with Docker Compose:

```bash
docker compose up -d
```

This will:
- Pull the JEAD inference server image
- Start the server on port 8090
- Mount your models directory
- Run in detached mode (background)

Check if the server is running:
```bash
docker compose ps
```

View server logs:
```bash
docker compose logs -f inference-server
```

#### Using NVIDIA GPU (compose.gpu.yaml)

If you want the inference server container to access an NVIDIA GPU, the distribution includes `compose.gpu.yaml` which adds the required device mapping (it sets `devices: - "nvidia.com/gpu=all"`). 
The `compose.gpu.yaml` has to be used as an extension to the base `compose.yaml` when starting the services, it cannot be used on its own.

Prerequisites:
- NVIDIA drivers installed on the host (verify with `nvidia-smi` on the host).
- NVIDIA Container Toolkit (a.k.a. nvidia-docker) configured so Docker can expose GPUs to containers. See the official docs: https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html
- A recent Docker Engine that supports the NVIDIA Container Toolkit. Restart the Docker daemon after installing the toolkit.

Quick verification that Docker can see the GPU (runs an official NVIDIA test image):

```bash
docker run --rm --gpus all nvidia/cuda:11.0-base nvidia-smi
```

Start the GPU-enabled compose in the following way:

```bash
docker compose -f compose.yaml -f compose.gpu.yaml up -d
```

Verify GPU usage:
- Check running containers:

```bash
docker compose ps
```

- To verify a container can see the GPU you can run the Nvidia test image shown above. If your inference server image contains `nvidia-smi` you may also exec into it:

```bash
docker exec -it <inference-server-container-name> nvidia-smi
```

### 3. Configure the Detector (Optional)

Edit `config/application.yml` if you need to customize:
- Inference server connection settings
- Analysis parameters
- Logging levels

## Running the Detector

### Basic Usage

Run the detector with:

```bash
java -jar detector.jar [options]
```

### With Custom Configuration

To use the provided configuration file:

```bash
java -jar detector.jar --spring.config.location=./config/application.yml
```

### Common Options

```bash
# Specify a different server port
java -jar detector.jar --server.port=8080
```

## Stopping the Services

### Stop the Detector
Press `Ctrl+C` if running in foreground, or kill the Java process.

### Stop the Inference Server
```bash
docker compose down
```

To also remove volumes:
```bash
docker compose down -v
```

## Troubleshooting

### Issue: "Unable to connect to inference server"
- Ensure the inference server is running: `docker compose ps`
- Check the server logs: `docker compose logs inference-server-cpu`
- Verify port 8081 is not blocked by firewall

### Issue: "Models not found"
- Verify `MODELS_ROOT_HOST` environment variable is set correctly
- Ensure the models directory exists and contains the required model files
- Check Docker volume mounting: `docker compose config`

### Issue: "Java version mismatch"
- Verify you have Java 25: `java -version`
- Update your `JAVA_HOME` environment variable if needed

### Issue: "Docker Compose not found"
- Try `docker-compose` (with hyphen) for older Docker installations
- Or upgrade to Docker Desktop which includes the newer `docker compose` command

## Architecture

```
┌─────────────────┐         ┌──────────────────────┐
│  JEAD Detector  │────────>│    Inference Server  │
│  (Spring Boot)  │  gRPC   │  (Docker Container)  │
│     Shell       │         │     Port: 8081       │
└─────────────────┘         └──────────────────────┘
                                      │
                                      v
                            ┌──────────────────┐
                            │     AI Models    │
                            │   (Host Volume)  │
                            └──────────────────┘
```

## Version

This distribution was built from version: **@project.version@**
