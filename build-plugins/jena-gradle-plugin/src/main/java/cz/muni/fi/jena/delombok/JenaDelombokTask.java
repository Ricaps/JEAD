package cz.muni.fi.jena.delombok;

import cz.muni.fi.jena.model.ProjectModel;
import cz.muni.fi.jena.plugin.delombok.Constants;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutor;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutorException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public abstract class JenaDelombokTask extends DefaultTask {

    private static final String COMPILE_CLASSPATH = "compileClasspath";
    private static final String DELOMBOK_SUFFIX = "delombok";

    public JenaDelombokTask() {
        getSourceDirectory().convention(getProject().getLayout().getProjectDirectory().dir("src"));
        getOutputDirectorySuffix().convention(DELOMBOK_SUFFIX);
        getFailIfNotFound().convention(true);
        getLombokArtifact().getGroupId().convention(Constants.DEFAULT_LOMBOK_GROUP_ID);
        getLombokArtifact().getArtifactId().convention(Constants.DEFAULT_LOMBOK_ARTIFACT_ID);
    }

    @InputDirectory
    public abstract DirectoryProperty getSourceDirectory();

    @Input
    public abstract Property<String> getOutputDirectorySuffix();

    @Input
    public abstract Property<Boolean> getFailIfNotFound();

    @Nested
    public abstract ProjectModel getLombokArtifact();

    @TaskAction
    public void delombok() {
        getLogger().lifecycle("Starting Jena Delombok execution...");

        Configuration config = getProject().getConfigurations().findByName(COMPILE_CLASSPATH);

        if (config == null) {
            throw new GradleException("Configuration '" + COMPILE_CLASSPATH + "' not found. Ensure the 'java' plugin is applied.");
        }

        ResolvedConfiguration resolvedConfiguration = config.getResolvedConfiguration();
        Set<ResolvedArtifact> resolvedArtifacts = resolvedConfiguration.getResolvedArtifacts();
        Optional<ResolvedArtifact> lombokOptional = findLombokJar(resolvedArtifacts);

        if (lombokOptional.isEmpty()) {
            throw new GradleException(String.format("Lombok dependency not found in '%s'. Delombok failed.", COMPILE_CLASSPATH));
        }

        File lombokJar = lombokOptional.get().getFile();
        File sourceDirectory = getSourceDirectory().get().getAsFile();

        DelombokExecutor delombokExecutor = new DelombokExecutor(sourceDirectory, lombokJar, getOutputDirectorySuffix().get());

        try {
            delombokExecutor.execute();
        } catch (DelombokExecutorException e) {
            throw new GradleException("Delombok execution failed!", e);
        }
    }

    private Optional<ResolvedArtifact> findLombokJar(Set<ResolvedArtifact> resolvedArtifacts) {
        ProjectModel referenceArtifact = getLombokArtifact();
        return resolvedArtifacts.stream().filter(resolvedArtifact ->
                        resolvedArtifact.getModuleVersion().getId().getGroup().equals(referenceArtifact.getGroupId().get()) &&
                                resolvedArtifact.getModuleVersion().getId().getName().equals(referenceArtifact.getArtifactId().get())
                )
                .findFirst();
    }
}
