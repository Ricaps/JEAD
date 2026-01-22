package cz.muni.jena.dependecies.finder;

import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

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
