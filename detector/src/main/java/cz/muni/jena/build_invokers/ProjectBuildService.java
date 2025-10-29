package cz.muni.jena.build_invokers;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
public class ProjectBuildService {

    private final List<BuildInvoker> buildInvokers;

    public ProjectBuildService(List<BuildInvoker> buildInvokers) {
        this.buildInvokers = buildInvokers;
    }

    /**
     * Runs build of Java project at given path(s).
     * <br>Picks the correct strategy for the needed build system (Maven, Gradle, ...) according to file suffix
     * @param paths collection of paths pointing to the build description files (pom.xml, build.gradle, ...)
     */
    public void runBuilds(Collection<String> paths) {
        var futures = paths.stream().flatMap(this::forEachInvoker).toArray(CompletableFuture[]::new);

        CompletableFuture<Void> future = CompletableFuture.allOf(futures);
        future.join();
    }

    private Stream<CompletableFuture<Void>> forEachInvoker(String stringPath) {
        Path path = Path.of(stringPath);

        return buildInvokers.stream()
                .filter(invoker -> invoker.canHandleBuild(path))
                .map(invoker -> executeInvoker(path, invoker));
    }

    private CompletableFuture<Void> executeInvoker(Path path, BuildInvoker buildInvoker) {
        return buildInvoker.runBuild(path);
    }
}
