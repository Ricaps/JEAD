package cz.muni.jena.configuration;

import org.testcontainers.containers.ComposeContainer;

import java.io.File;
import java.nio.file.Paths;

public class TestContainers {
    public static final String COMPOSE_PATH = "../compose.test.yml";
    public static final String DATABASE_SERVICE_NAME = "db";
    public static final int DATABASE_SERVICE_PORT = 5001;
    public static final String MODELS_ROOT_ENV = "MODELS_ROOT_HOST";
    public static final String MODELS_ROOT_ENV_VALUE = Paths.get("src", "test", "resources", "fake_models_root").toFile().getAbsolutePath();
    public static final String INFERENCE_SERVER_SERVICE_NAME = "inference-server-cpu";
    public static final int INFERENCE_SERVICE_PORT = 5002;

    public static ComposeContainer getComposeContainer() {
        return new ComposeContainer(
                new File(COMPOSE_PATH)
        )
                .withLocalCompose(true)
                .withExposedService(DATABASE_SERVICE_NAME, DATABASE_SERVICE_PORT)
                .withEnv("POSTGRES_USER", "root")
                .withEnv("POSTGRES_PASSWORD", "detector")
                .withEnv("POSTGRES_DB", "antipatternDetector")
                    .withEnv(MODELS_ROOT_ENV, MODELS_ROOT_ENV_VALUE)
                .withExposedService(INFERENCE_SERVER_SERVICE_NAME, INFERENCE_SERVICE_PORT);
    }
}
