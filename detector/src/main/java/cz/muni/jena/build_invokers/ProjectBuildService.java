package cz.muni.jena.build_invokers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
public class ProjectBuildService {

    private final ProjectFinderService projectFinderService;
    private final List<BuildInvoker> buildInvokers;

    @Autowired
    public ProjectBuildService(ProjectFinderService projectFinderService, List<BuildInvoker> buildInvokers) {
        this.projectFinderService = projectFinderService;
        this.buildInvokers = buildInvokers;
    }

    /**
     * Runs build of Java project at given path(s).
     * <br>Picks the correct strategy for the needed build system (Maven, Gradle, ...) according to file suffix
     * @param basePath path from where the algorithm should look like for build description files / projects
     */
    public void runBuilds(Path basePath) {
        var futures = projectFinderService.find(basePath)
                .stream()
                .flatMap(this::forEachInvoker)
                .toArray(CompletableFuture[]::new);

        CompletableFuture<Void> future = CompletableFuture.allOf(futures);
        future.join();
    }

    private Stream<CompletableFuture<Void>> forEachInvoker(Path path) {

        return buildInvokers.stream()
                .filter(invoker -> invoker.canHandleBuild(path))
                .map(invoker -> executeInvoker(path, invoker));
    }

    private CompletableFuture<Void> executeInvoker(Path path, BuildInvoker buildInvoker) {
        return buildInvoker.runBuild(path);
    }
}
