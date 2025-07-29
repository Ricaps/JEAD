package cz.muni.jena.frontend.commands.project.preparation;

import java.util.Collection;

public record PomAndGradleFiles(Collection<String> mavenFiles, Collection<String> gradleFiles)
{
}
