package cz.muni.jena.parser;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AsyncCompilationUnitParser {
    public static final String DELOMBOK_SUFFIX = "-delombok";
    private static final String TARGET_FOLDER = "target";
    private static final String POM_XML = "pom.xml";
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String UNKNOWN_PROJECT = "UNKNOWN";
    private final Path path;
    private final TypeSolver typeSolver;

    public AsyncCompilationUnitParser(
            Path path,
            Supplier<List<TypeSolver>> typeSolversSupplier
    ) {
        this.path = path;
        List<TypeSolver> typeSolvers = typeSolversSupplier.get();
        this.typeSolver = new CombinedTypeSolver(typeSolvers);
        logTypeSolvers(path, typeSolvers);
    }

    public AsyncCompilationUnitParser(String path) {
        this(FileSystems.getDefault().getPath(path));
    }

    public AsyncCompilationUnitParser(Path path) {
        this(path, new TypeSolverSupplier(path));
    }

    private void logTypeSolvers(Path path, List<TypeSolver> typeSolvers) {
        Logger logger = LoggerFactory.getLogger(TypeSolverSupplier.class);
        long jarTypeSolversCount = typeSolvers.stream()
                .filter(JarTypeSolver.class::isInstance)
                .count();
        if (jarTypeSolversCount == 0L) {
            logger.atError().log(
                    String.format(
                            """
                                    We failed to find any dependency Jars in %s.
                                    Without dependency Jars Jena will likely not function properly.
                                    """,
                            path.toAbsolutePath() + File.separator + "target" + File.separator + "dependency"
                    ));
        } else {
            logger.atInfo().log(String.format("We found %s dependency Jars.", jarTypeSolversCount));
        }
    }

    public void processCompilationUnits(SourceRoot.Callback callback) {
        ParserConfiguration parserConfig = new ParserConfiguration();
        JavaSymbolSolver symbolResolver = new JavaSymbolSolver(typeSolver);
        parserConfig.setSymbolResolver(symbolResolver);
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy(parserConfig).collect(path);
        List<SourceRoot> sourceRoots = getFilteredSourceRoot(projectRoot);
        Logger logger = LoggerFactory.getLogger(AsyncCompilationUnitParser.class);
        logger.atInfo().log(
                "We will analyze following modules: "
                        + System.lineSeparator()
                        + sourceRoots
                        .stream()
                        .map(SourceRoot::getRoot)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .collect(Collectors.joining(System.lineSeparator()))
        );
        for (SourceRoot sourceRoot : sourceRoots) {
            try {
                sourceRoot.parseParallelized(callback);
            } catch (IOException ignored) {
                logger.atWarn().log("We weren't able to parse following module: " + sourceRoot.getRoot().toAbsolutePath());
            }
        }
    }

    /**
     * Since jena-maven-plugin (gradle plugin) creates delomboked folder with (-delombok) suffix, we have to filter out
     * original source codes. <br>
     * This method iterates through all source roots and filters out original paths to the source roots which has its delomboked "copy".
     * @param projectRoot project root
     * @return filtered source roots
     */
    private List<SourceRoot> getFilteredSourceRoot(ProjectRoot projectRoot) {
        List<Path> delombokedOriginalRoots = new ArrayList<>();

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            Optional<Path> delombokPathOpt = getDelombokPath(sourceRoot.getRoot());

            delombokPathOpt.ifPresent(delombokPath -> {
                String delombokPathWithoutSuffix = delombokPath.toString().replace(DELOMBOK_SUFFIX, "");
                delombokedOriginalRoots.add(Path.of(delombokPathWithoutSuffix));
            });
        }

        return projectRoot.getSourceRoots().stream()
                .filter(sourceRoot -> !delombokedOriginalRoots.contains(sourceRoot.getRoot()))
                .toList();
    }

    private String getProjectName(Path sourceRoot) {
        Path currentPath = sourceRoot;

        while (currentPath != null) {
            Path fileName = currentPath.getFileName().getFileName();
            if (isProjectRoot(fileName)) {
                return currentPath.getParent().getFileName().toString();
            }

            currentPath = currentPath.getParent();
        }

        return UNKNOWN_PROJECT;
    }

    private boolean isProjectRoot(Path file) {
        Path fileName = file.getFileName().getFileName();
        if (fileName.toString().equals(TARGET_FOLDER)) {
            return true;
        }

        Path pomFile = fileName.resolve(POM_XML);
        if (pomFile.toFile().exists()) {
            return true;
        }

        Path buildGradle = fileName.resolve(BUILD_GRADLE);
        return buildGradle.toFile().exists();
    }

    private Optional<Path> getDelombokPath(Path sourceRoot) {
        Path currentPath = sourceRoot;

        while (currentPath != null) {
            Path fileName = currentPath.getFileName();
            if (fileName != null && fileName.toString().endsWith(DELOMBOK_SUFFIX)) {
                return Optional.of(sourceRoot);
            }

            currentPath = currentPath.getParent();
        }

        return Optional.empty();
    }
}
