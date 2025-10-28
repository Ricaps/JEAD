package cz.muni.jena.build_invokers;

import jakarta.annotation.Nonnull;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class MavenInvoker extends AbstractBuildInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenInvoker.class);
    private static final String POM_XML = "pom.xml";

    @Override
    protected void run(@Nonnull Path path) {
        InvocationRequest invocationRequest = new DefaultInvocationRequest();
        invocationRequest.setPomFile(path.toFile());
        invocationRequest.addArgs(List.of("clean", "package", "-DskipTests"));
        invocationRequest.setQuiet(true);

        Invoker invoker = new DefaultInvoker();

        LOGGER.info("Starting building project '{}' using Maven: '{}'",
                path,
                invoker.getMavenExecutable());

        try {
            InvocationResult result = invoker.execute(invocationRequest);

            if (result.getExitCode() == 0) {
                LOGGER.info("Successfully built project {}", path);
            } else {
                LOGGER.error("Error occurred while building project {}", path, result.getExecutionException());
            }
        } catch (MavenInvocationException e) {
            LOGGER.error("Failed to build project {}", path, e);
        }
    }

    @Override
    public boolean canHandleBuild(Path path) {
        return path.endsWith(POM_XML);
    }
}
