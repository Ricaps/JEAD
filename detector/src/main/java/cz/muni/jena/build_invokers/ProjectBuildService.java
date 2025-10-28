package cz.muni.jena.build_invokers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
