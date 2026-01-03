package cz.muni.fi.jena;

import cz.muni.fi.jena.delombok.JenaDelombokTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class JenaGradlePlugin implements Plugin<Project> {

    private static final String DELOMBOK_TASK = "delombok";

    @Override
    public void apply(@NotNull Project target) {
        target.getTasks().register(DELOMBOK_TASK, JenaDelombokTask.class);
    }
    
}
