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

4. **Download Models Root** (if not already provided)
    - Download models root from [Google Drive](https://drive.google.com/file/d/1WdbWYgl9AGuR9D05vGyUwJABdJHeCPNU/view?usp=sharing) !NOT RELEVANT FOR THESIS SUBMISSION! (models root is included in the thesis attachment)
    - Unzip the downloaded file
    - Copy path to the models root folder in the unzipped archive

## Package Contents

This distribution contains:

- `detector.jar` - The main JEAD detector application (Spring Boot executable JAR)
- `compose.yaml` - Docker Compose configuration for the AI inference server
- `config/application.yml` - Configuration file for the detector
- `.env` - Environment configuration file (edit to set your models path)
- `README.md` - This file

## Setup

### 1. Configure Models Path and GPU

Before starting, you need to configure the `.env` file. Open it and set the following variables:

```bash
MODELS_ROOT_HOST=/path/to/your/models
USE_GPU=false
```

- **`MODELS_ROOT_HOST`** — Replace `/path/to/your/models` with the absolute path to your models directory on your host machine.
- **`USE_GPU`** — Set to `true` to enable GPU acceleration inside the inference server container, or leave as `false` to use CPU only. 
When set to `true`, you must start the inference server using the GPU-enabled compose command (see [Using NVIDIA GPU](#using-nvidia-gpu-composegpuyaml) below) instead of the plain `docker compose up -d`.

### 2. Configure GitHub Packages Access Token

To download JEAD plugins from GitHub Packages, configure the repository token in `application.yml` under `prepare-plugin.repository.access-token`.

The token must be a **GitHub Classic Personal Access Token** with at least the `read:packages` permission. See [GitHub documentation for creating a PAT](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic)

```yaml
prepare-plugin:
  repository:
    id: jead-github
    url: https://x-access-token:${prepare-plugin.repository.access-token}@maven.pkg.github.com/Ricaps/JEAD
    access-token: <your-github-classic-pat-with-read-packages>
```

This token will be inserted to the analyzed projects' build files when you run the `prepareProjects` command, allowing them to download the necessary JEAD plugins.

### 3. Start the AI Inference Server

The detector requires the inference server to be running. Start it with Docker Compose:

```bash
docker compose up
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
- Set `USE_GPU=true` in the `.env` file (it defaults to `false`).
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

### 4. Configure the Detector (Optional)

Edit `config/application.yml` if you need to customize:
- Inference server connection settings
- Analysis parameters
- Logging levels

### 5. Prepare Target Projects for Analysis

Before running `detectIssues`, the analyzed project must have dependencies available in `target/dependency`.

Use JEAD's `prepareProjects` command (inside the detector shell) to insert preparation into target projects:

```bash
prepareProjects -d /absolute/path/to/Sample01
```

Or prepare all projects in a parent directory:

```bash
prepareProjects -d /absolute/path/to/workspace
```

What `prepareProjects` does:
- For Maven projects, it inserts JEAD plugin configuration so you can run package and copy dependencies.
- For Gradle projects, it appends a JEAD build script that adds repository/plugin setup and required JEAD tasks.

After preparation, build each target project (Maven package or Gradle JEAD tasks), then verify jars exist in `target/dependency`.

Then analyze the project in the detector shell:

```bash
detectIssues -p /absolute/path/to/Sample01
```

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
