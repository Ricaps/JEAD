package cz.muni.fi.jead.dependencies;

import cz.muni.fi.jead.plugin.copy_dependencies.CopyDependenciesException;
import cz.muni.fi.jead.plugin.copy_dependencies.CopyDependenciesExecutor;
import cz.muni.fi.jead.utils.ProjectUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Gradle task that copies all resolved project dependencies to a specified output directory.
 * This task is designed to be used within the Jena Gradle plugin.
 */
public abstract class JeadCopyDependenciesTask extends DefaultTask {

    public JeadCopyDependenciesTask() {
        Project project = getProject();
        Directory projectDirectory = project.getLayout().getProjectDirectory();
        getOutputFolder().convention(projectDirectory.dir("target").dir("dependencies"));

        getArtifactFiles().from(project.provider(() -> {
            Set<ResolvedArtifact> resolvedArtifacts = ProjectUtils.getResolvedArtifacts(project);
            return resolvedArtifacts.stream()
                    .map(ResolvedArtifact::getFile)
                    .filter(file -> file.getName().endsWith(".jar"))
                    .collect(Collectors.toSet());
        }));
    }

    /**
     * The directory where the resolved dependencies will be copied.
     * Defaults to `project.projectDir/target/dependencies`.
     *
     * @return A {@link DirectoryProperty} representing the output folder.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputFolder();

    /**
     * The dependencies to be copied.
     *
     * @return A {@link ConfigurableFileCollection} containing the artifact files.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getArtifactFiles();

    @TaskAction
    public void copyDependencies() {
        getLogger().lifecycle("Starting Jena Copy Dependencies execution...");

        Set<File> artifacts = getArtifactFiles().getFiles();

        Path outputPath = getOutputFolder().get().getAsFile().toPath();
        CopyDependenciesExecutor copyDependenciesExecutor = new CopyDependenciesExecutor(outputPath, artifacts);

        try {
            copyDependenciesExecutor.execute();
        } catch (CopyDependenciesException e) {
            throw new GradleException("Failed to copy dependencies!", e);
        }
    }
}
