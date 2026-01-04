package cz.muni.fi.jena.delombok;

import cz.muni.fi.jena.model.ProjectModel;
import cz.muni.fi.jena.plugin.delombok.Constants;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutor;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutorException;
import cz.muni.fi.jena.utils.ProjectUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Optional;
import java.util.Set;

/**
 * A Gradle task that delomboks Java source files.
 * This task uses the Lombok library to process source files and remove Lombok annotations,
 * generating plain Java code.
 */
public abstract class JenaDelombokTask extends DefaultTask {

    private static final String DELOMBOK_SUFFIX = "delombok";

    public JenaDelombokTask() {
        Project project = getProject();
        getSourceDirectory().convention(project.provider(() -> {
            Directory defaultDirectory = project.getLayout().getProjectDirectory().dir("src");
            File directoryAsFile = defaultDirectory.getAsFile();

            return directoryAsFile.exists() ? defaultDirectory : null;
        }));
        getOutputDirectorySuffix().convention(DELOMBOK_SUFFIX);
        getFailIfNotFound().convention(false);
        getLombokArtifact().getGroupId().convention(Constants.DEFAULT_LOMBOK_GROUP_ID);
        getLombokArtifact().getArtifactId().convention(Constants.DEFAULT_LOMBOK_ARTIFACT_ID);

        getLombokJarFile().convention(project.getLayout().file(project.provider(() -> {
            Set<ResolvedArtifact> resolvedArtifacts = ProjectUtils.getResolvedArtifacts(project);
            return findLombokJar(resolvedArtifacts)
                    .map(ResolvedArtifact::getFile)
                    .orElse(null);
        })));
    }

    /**
     * Returns the directory containing the source files to be delomboked.
     * The default value is "src" relative to the project directory.
     *
     * @return The source directory property.
     */
    @InputDirectory
    @org.gradle.api.tasks.Optional
    public abstract DirectoryProperty getSourceDirectory();

    /**
     * Returns the suffix to be appended to the output directory name.
     * The delomboked files will be placed in a directory named after the source directory
     * with this suffix appended (e.g., "src-delombok").
     * The default value is "delombok".
     *
     * @return The output directory suffix property.
     */
    @Input
    public abstract Property<String> getOutputDirectorySuffix();

    @Input
    public abstract Property<Boolean> getFailIfNotFound();

    /**
     * Returns the Lombok artifact configuration.
     * This nested property allows specifying the groupId and artifactId of the Lombok dependency
     * to be used for delomboking.
     */
    @Nested
    public abstract ProjectModel getLombokArtifact();

    @InputFile
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @org.gradle.api.tasks.Optional
    public abstract RegularFileProperty getLombokJarFile();

    @TaskAction
    public void delombok() {
        getLogger().lifecycle("Starting Jena Delombok execution...");

        Boolean failIfNotFound = getFailIfNotFound().get();
        if (!getLombokJarFile().isPresent() && failIfNotFound) {
            throw new GradleException(String.format("Lombok dependency not found in '%s'. Delombok failed.", ProjectUtils.COMPILE_CLASSPATH));
        } else if (!getLombokJarFile().isPresent()) {
            getLogger().warn("Lombok dependency not found in '{}'. Delombok skipped...", ProjectUtils.COMPILE_CLASSPATH);
            return;
        }

        File lombokJar = getLombokJarFile().get().getAsFile();
        if (!getSourceDirectory().isPresent()) {
            getLogger().warn("Skipping task execution because source directory doesn't exist");
            return;
        }
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
