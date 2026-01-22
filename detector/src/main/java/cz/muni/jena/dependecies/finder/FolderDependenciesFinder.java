package cz.muni.jena.dependecies.finder;

import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Objects;

public class FolderDependenciesFinder implements DependenciesFinder {

    private static final PathMatcher TARGET_DEPENDENCY_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/target/dependencies/**.jar");

    @Override
    public List<JarTypeSolver> findJarTypeSolvers(String projectPath) {

        try (var stream = Files.walk(Path.of(projectPath))) {
            return stream
                    .filter(TARGET_DEPENDENCY_MATCHER::matches)
                    .map(file -> {
                        try {
                            return new JarTypeSolver(file);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
