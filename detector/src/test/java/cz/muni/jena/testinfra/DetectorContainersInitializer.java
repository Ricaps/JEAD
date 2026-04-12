package cz.muni.jena.testinfra;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class DetectorContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        SharedDetectorContainers.start();

        TestPropertyValues.of(
                "spring.datasource.url=" + SharedDetectorContainers.jdbcUrl(),
                "spring.datasource.username=" + SharedDetectorContainers.dbUsername(),
                "spring.datasource.password=" + SharedDetectorContainers.dbPassword(),
                "spring.grpc.client.channels.inference-server.address=" + SharedDetectorContainers.inferenceAddress()
        ).applyTo(applicationContext.getEnvironment());
    }
}

