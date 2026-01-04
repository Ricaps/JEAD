package cz.muni.fi.jena.utils;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectUtils {

    public static final String COMPILE_CLASSPATH = "compileClasspath";
    public static final String RUNTIME_CLASSPATH = "runtimeClasspath";

    private ProjectUtils() {
        super();
    }

    public static Set<ResolvedArtifact> getResolvedArtifacts(Project project) {
        Stream<ResolvedArtifact> artifactsProject = getResolvedArtifactStream(project);

        return artifactsProject.collect(Collectors.toSet());
    }

    private static Stream<ResolvedArtifact> getResolvedArtifactStream(Project project) {
        Set<ResolvedArtifact> compileClasspath = getArtifacts(project, COMPILE_CLASSPATH);
        Set<ResolvedArtifact> runtimeClasspath = getArtifacts(project, RUNTIME_CLASSPATH);

        return Stream.concat(compileClasspath.stream(), runtimeClasspath.stream());
    }

    private static Set<ResolvedArtifact> getArtifacts(Project subproject, String classPath) {
        Configuration config = subproject.getConfigurations().findByName(classPath);

        if (config == null) {
            return Set.of();
        }

        ResolvedConfiguration resolvedConfiguration = config.getResolvedConfiguration();
        return resolvedConfiguration.getResolvedArtifacts();
    }
}