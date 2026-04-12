package cz.muni.jena.testinfra;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

public class DetectorContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        SharedDetectorContainers.start();
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.datasource.url=" + SharedDetectorContainers.postgresJdbcUrl(),
                "spring.datasource.username=" + SharedDetectorContainers.postgresUsername(),
                "spring.datasource.password=" + SharedDetectorContainers.postgresPassword(),
                "spring.grpc.client.channels.inference-server.address=" + SharedDetectorContainers.inferenceAddress()
        );
    }
}

