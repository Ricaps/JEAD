package cz.muni.fi.jena.dependencies;

import cz.muni.fi.jena.plugin.copy_dependencies.CopyDependenciesException;
import cz.muni.fi.jena.plugin.copy_dependencies.CopyDependenciesExecutor;
import cz.muni.fi.jena.utils.ProjectUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Gradle task that copies all resolved project dependencies to a specified output directory.
 * This task is designed to be used within the Jena Gradle plugin.
 */
public abstract class JenaCopyDependenciesTask extends DefaultTask {

    /**
     * The directory where the resolved dependencies will be copied.
     * Defaults to `project.projectDir/target/dependencies`.
     *
     * @return A {@link DirectoryProperty} representing the output folder.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputFolder();

    public JenaCopyDependenciesTask() {
        Directory projectDirectory = getProject().getLayout().getProjectDirectory();
        getOutputFolder().convention(projectDirectory.dir("target").dir("dependencies"));
    }

    @TaskAction
    public void copyDependencies() {
        getLogger().lifecycle("Starting Jena Copy Dependencies execution...");

        Set<ResolvedArtifact> resolvedArtifacts = ProjectUtils.getResolvedArtifacts(getProject());
        Set<File> artifacts = resolvedArtifacts.stream().map(ResolvedArtifact::getFile).collect(Collectors.toSet());

        Path outputPath = getOutputFolder().get().getAsFile().toPath();
        CopyDependenciesExecutor copyDependenciesExecutor = new CopyDependenciesExecutor(outputPath, artifacts);

        try {
            copyDependenciesExecutor.execute();
        } catch (CopyDependenciesException e) {
            throw new GradleException("Failed to copy dependencies!", e);
        }
    }
}
