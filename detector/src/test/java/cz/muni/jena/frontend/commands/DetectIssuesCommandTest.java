package cz.muni.jena.frontend.commands;

import cz.muni.jena.grpc.InferenceServiceGrpc;
import cz.muni.jena.grpc.ServerReadyRequest;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.shell.interactive.enabled=false",
                "spring.shell.script.enabled=false"
        }
)
public class DetectIssuesCommandTest {

    @Container
    @SuppressWarnings("unused")
    public static final ComposeContainer composeContainer = new ComposeContainer(
            new File("../compose.test.yml")
    )
            .withLocalCompose(true)
            .withExposedService("db", 5432).withEnv("MODELS_ROOT_HOST", "/media/martin/BigData/models/models_root/")
            .withExposedService("inference-server-cpu", 8080);


    @Autowired
    InferenceServiceGrpc.InferenceServiceBlockingV2Stub stub;


    @Test
    void test01() throws StatusException {
        assertThat(stub.serverReady(ServerReadyRequest.newBuilder().build()).getReady()).isTrue();
    }

}