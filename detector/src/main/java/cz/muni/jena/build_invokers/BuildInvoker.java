package cz.muni.jena.build_invokers;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface BuildInvoker {

    /**
     * Runs build asynchronously using current build invoker
     * @param path path to the build description file (pom.xml, build.gradle, ...)
     */
    CompletableFuture<Void> runBuild(@Nonnull Path path);

    /**
     * Checks if current invoker can handle building using specific technology
     * @param path path to the build description (pom.xml, build.gradle)
     * @return true is current invoker can handle build
     */
    boolean canHandleBuild(Path path);

}
