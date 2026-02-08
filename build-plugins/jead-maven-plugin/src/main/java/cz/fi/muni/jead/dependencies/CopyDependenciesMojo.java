package cz.fi.muni.jead.dependencies;

import cz.muni.fi.jead.plugin.copy_dependencies.CopyDependenciesException;
import cz.muni.fi.jead.plugin.copy_dependencies.CopyDependenciesExecutor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "copy-dependencies", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class CopyDependenciesMojo
        extends AbstractMojo {

    /**
     * Target path where to copy .jar dependencies of the project
     */
    @Parameter(property = "jena.dependenciesFolder", defaultValue = "${project.basedir}/target/dependencies")
    private String outputFolder;

    @Parameter(property = "project", readonly = true)
    private MavenProject mavenProject;

    public void execute()
            throws MojoExecutionException {

        Set<File> artifacts = mavenProject.getArtifacts().stream().map(Artifact::getFile).collect(Collectors.toSet());

        CopyDependenciesExecutor copyDependenciesExecutor = new CopyDependenciesExecutor(Path.of(outputFolder), artifacts);
        try {
            copyDependenciesExecutor.execute();
        } catch (CopyDependenciesException e) {
            throw new MojoExecutionException("Failed to copy dependencies!", e);
        }
    }
}
