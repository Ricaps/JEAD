package cz.muni.jena.build_invokers;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

@Service
public class ProjectFinderService {

    private final List<ProjectFinder> projectFinders;

    protected ProjectFinderService(List<ProjectFinder> projectFinders) {
        this.projectFinders = projectFinders;
    }

    private static List<Path> appendPathToResult(Path file, List<Path> pathList) {
        if (pathList == null) {
            pathList = new ArrayList<>();
        }

        pathList.add(file);

        return pathList;
    }

    public List<Path> find(Path basePath) {
        Map<ProjectFinder, String> fileSuffixMap = getFileSuffixMap();
        Map<ProjectFinder, List<Path>> result = new HashMap<>();

        try {
            Files.walkFileTree(basePath, Set.of(FOLLOW_LINKS), 2, new BuildFileVisitor(fileSuffixMap, result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return projectFinders.stream().flatMap(finder -> {
            List<Path> paths = result.get(finder);
            if (paths == null) {
                return Stream.of();
            }
            return finder.process(paths).stream();
        }).toList();
    }

    private Map<ProjectFinder, String> getFileSuffixMap() {
        return projectFinders.stream()
                .map(finder -> Map.entry(finder, finder.getFileSuffix()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static class BuildFileVisitor extends SimpleFileVisitor<Path> {

        private final Map<ProjectFinder, String> fileSuffixMap;
        private final Map<ProjectFinder, List<Path>> result;

        public BuildFileVisitor(Map<ProjectFinder, String> fileSuffixMap, Map<ProjectFinder, List<Path>> result) {
            this.fileSuffixMap = fileSuffixMap;
            this.result = result;
        }

        @Override
        @Nonnull
        public FileVisitResult visitFile(Path file, @Nonnull BasicFileAttributes attrs) {
            if (Files.isDirectory(file)) {
                return FileVisitResult.CONTINUE;
            }

            fileSuffixMap.forEach((finder, suffix) -> {
                if (file.endsWith(suffix)) {
                    result.compute(finder, (projectFinder, pathList) -> appendPathToResult(file, pathList));
                }
            });

            return FileVisitResult.CONTINUE;
        }
    }
}
