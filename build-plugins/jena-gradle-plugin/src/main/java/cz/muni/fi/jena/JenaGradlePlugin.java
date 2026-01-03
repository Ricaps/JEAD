package cz.muni.fi.jena;

import cz.muni.fi.jena.delombok.JenaDelombokTask;
import cz.muni.fi.jena.dependencies.JenaCopyDependenciesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class JenaGradlePlugin implements Plugin<Project> {

    public static final String DELOMBOK_TASK = "delombok";
    public static final String COPY_DEPENDENCIES_TASK = "copyDependencies";

    @Override
    public void apply(@NotNull Project target) {
        target.getTasks().register(DELOMBOK_TASK, JenaDelombokTask.class);
        target.getTasks().register(COPY_DEPENDENCIES_TASK, JenaCopyDependenciesTask.class);
    }
    
}
