package cz.muni.fi.jead;

import cz.muni.fi.jead.delombok.JeadDelombokTask;
import cz.muni.fi.jead.dependencies.JeadCopyDependenciesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class JeadGradlePlugin implements Plugin<Project> {

    public static final String DELOMBOK_TASK = "jeadDelombok";
    public static final String COPY_DEPENDENCIES_TASK = "jeadCopyDependencies";

    @Override
    public void apply(@NotNull Project target) {
        target.getTasks().register(DELOMBOK_TASK, JeadDelombokTask.class);
        target.getTasks().register(COPY_DEPENDENCIES_TASK, JeadCopyDependenciesTask.class);
    }
    
}
