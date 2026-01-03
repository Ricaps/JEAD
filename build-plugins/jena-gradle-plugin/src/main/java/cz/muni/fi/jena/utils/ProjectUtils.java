package cz.muni.fi.jena.utils;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;

import java.util.Set;

public class ProjectUtils {

    public static final String COMPILE_CLASSPATH = "compileClasspath";

    private ProjectUtils() {
        super();
    }

    public static Set<ResolvedArtifact> getResolvedArtifacts(Project project) {
        Configuration config = project.getConfigurations().findByName(COMPILE_CLASSPATH);

        if (config == null) {
            throw new GradleException("Configuration '" + COMPILE_CLASSPATH + "' not found. Ensure the 'java' plugin is applied.");
        }

        ResolvedConfiguration resolvedConfiguration = config.getResolvedConfiguration();
        return resolvedConfiguration.getResolvedArtifacts();
    }
}