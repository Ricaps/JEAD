package cz.fi.muni.jena.dependencies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

@Mojo(name = "copy-dependencies", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class CopyDependenciesMojo
        extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyDependenciesMojo.class);

    /**
     * Target path where to copy .jar dependencies of the project
     */
    @Parameter(property = "jena.dependenciesFolder", defaultValue = "${project.basedir}/target/dependencies")
    private String outputFolder;

    @Parameter(property = "project", readonly = true)
    private MavenProject mavenProject;

    public void execute()
            throws MojoExecutionException {
        Path targetPath = Path.of(outputFolder);
        if (!targetPath.toFile().exists()) {
            createTargetDirectory(targetPath);
        }

        AtomicInteger count = new AtomicInteger();
        for (Artifact artifact : mavenProject.getArtifacts()) {
            File file = artifact.getFile();
            try {
                Path newPath = Files.copy(Path.of(file.getPath()), targetPath.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                count.addAndGet(1);
                LOGGER.info("Copied dependency {} to path {}", artifact.getArtifactId(), newPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy dependencies!", e);
            }
        }

        LOGGER.info("Copied {} dependencies to directory {}", count, outputFolder);
    }

    private static void createTargetDirectory(Path targetPath) throws MojoExecutionException {
        try {
            Path createdDirectory = Files.createDirectories(targetPath);
            LOGGER.info("Created directory for dependencies {}", createdDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create target directory!", e);
        }
    }
}
