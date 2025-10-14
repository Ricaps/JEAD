package cz.muni.jena.inference;

import cz.muni.jena.configuration.TestContainers;
import cz.muni.jena.utils.NonShellIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

@NonShellIntegrationTest
class InferenceServiceIT {

    @Container
    @SuppressWarnings("unused")
    private static final ComposeContainer composeContainer = TestContainers.getComposeContainer();

    @Autowired
    InferenceService inferenceService;

    @Test
    void inferenceService_serverIsReady_returnsTrue(){
        assertThat(inferenceService.isServerReady()).isTrue();
    }
}