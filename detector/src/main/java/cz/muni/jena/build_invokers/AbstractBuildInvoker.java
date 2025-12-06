package cz.muni.jena.build_invokers;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractBuildInvoker implements BuildInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBuildInvoker.class);

    @Async("buildInvokerExecutor")
    @Override
    public CompletableFuture<Void> runBuild(@Nonnull Path path) {
        try {
            run(path);
        } catch (Exception e) {
            LOGGER.error("Failed to build project {}", path, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    protected abstract void run(@Nonnull Path path);
}
