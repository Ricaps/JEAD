package cz.muni.jena.frontend.commands.project.preparation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record PomAndGradleFiles(Collection<String> mavenFiles, Collection<String> gradleFiles)
{
    public List<String> getAllFiles() {
        return Stream.concat(mavenFiles.stream(), gradleFiles.stream()).toList();
    }
}
