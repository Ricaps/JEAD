package cz.muni.jena.testinfra;

import cz.muni.jena.inference.InferenceFacade;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("resource")
public final class SharedDetectorContainers {

    private static final Path WORKSPACE_ROOT = Path.of(System.getProperty("user.dir")).getParent();
    private static final Path INFERENCE_SERVER_DIR = WORKSPACE_ROOT.resolve("ai-detector/inference-server");
    private static final Path INFERENCE_DOCKERFILE = INFERENCE_SERVER_DIR.resolve("Dockerfile");
    private static final Path MODEL_ROOT = WORKSPACE_ROOT.resolve("ai-detector/inference-server/tests/resources/model_root");

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("antipatternDetector")
            .withUsername("root")
            .withPassword("detector");

    private static final GenericContainer<?> INFERENCE_SERVER = new GenericContainer<>(
            new ImageFromDockerfile("jead-inference-local-it", false)
                    .withDockerfile(INFERENCE_DOCKERFILE)
    )
            .withExposedPorts(8080)
            .withEnv("ADDRESS", "0.0.0.0")
            .withEnv("PORT", "8080")
            .withEnv("MODELS_ROOT", "/usr/models")
            .withEnv("USE_GPU", "false")
            .withCopyFileToContainer(MountableFile.forHostPath(MODEL_ROOT), "/usr/models")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(3));

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private SharedDetectorContainers() {
    }

    public static void start() {
        validatePrerequisites();
        if (STARTED.compareAndSet(false, true)) {
            INFERENCE_SERVER.start();
            POSTGRES.start();
        }
    }

    public static String postgresJdbcUrl() {
        start();
        return POSTGRES.getJdbcUrl();
    }

    public static String postgresUsername() {
        start();
        return POSTGRES.getUsername();
    }

    public static String postgresPassword() {
        start();
        return POSTGRES.getPassword();
    }

    public static String inferenceAddress() {
        start();
        return INFERENCE_SERVER.getHost() + ":" + INFERENCE_SERVER.getMappedPort(8080);
    }

    public static void waitForInferenceReady(InferenceFacade inferenceFacade, Duration timeoutDuration) {
        Instant timeout = Instant.now().plus(timeoutDuration);
        while (Instant.now().isBefore(timeout)) {
            if (inferenceFacade.canUseMachineLearning()) {
                return;
            }
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for inference server", e);
            }
        }
        throw new IllegalStateException("Inference server was not ready in time");
    }

    private static void validatePrerequisites() {
        if (!Files.isRegularFile(INFERENCE_DOCKERFILE)) {
            throw new IllegalStateException("Inference Dockerfile does not exist: " + INFERENCE_DOCKERFILE.toAbsolutePath());
        }
        if (!Files.isDirectory(MODEL_ROOT)) {
            throw new IllegalStateException("Model root does not exist: " + MODEL_ROOT.toAbsolutePath());
        }
    }
}

