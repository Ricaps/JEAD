package cz.fi.muni.jena.delombok;


import cz.fi.muni.jena.model.ProjectModel;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutor;
import cz.muni.fi.jena.plugin.delombok.DelombokExecutorException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

@Mojo(name = "delombok", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute
public class DelombokMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelombokMojo.class);
    private static final String LOMBOK_ARTIFACT_ID = "lombok";
    private static final String LOMBOK_GROUP_ID = "org.projectlombok";

    /**
     * Path to the folder with exposed .jar dependencies
     */
    @Parameter(property = "jena.dependenciesFolder", defaultValue = "${project.basedir}/target/dependencies")
    private String dependenciesFolder;

    /**
     * Source code directory which should be delomboked
     */
    @Parameter(property = "jena.delombok.sourceDirectory", defaultValue = "${project.basedir}/src")
    private String sourceDirectory;

    /**
     * Suffix of the directory where the delomboked source codes should be stored. <br>
     * The suffix is added to the name of the sourceDirectory. <br>
     * When source directory is 'src', then output directory will be 'src-delombok'
     */
    @Parameter(property = "jena.delombok.outputDirectory", defaultValue = "delombok")
    private String outputDirectorySuffix;

    /**
     * If the maven should fail when the lombok jar is not found
     */
    @Parameter(property = "jena.delombok.failIfNotFound", defaultValue = "true")
    private boolean failIfNotFound;

    /**
     * Settings to customize lombok artifactId and groupId
     */
    @Parameter(property = "jena.lombok-artifact-id")
    private ProjectModel lombok = new ProjectModel();

    /**
     * Decides if execution of this goal should be skipped
     */
    @Parameter(property = "jena.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "project", readonly = true)
    private MavenProject mavenProject;


    @Override
    public void execute() throws MojoExecutionException {
        if (mavenProject.getPackaging().equalsIgnoreCase("pom")) {
            LOGGER.info("Skipped project {} because 'pom' packaging is not supported.", mavenProject.getArtifact().getArtifactId());
            return;
        }

        if (skip) {
            LOGGER.info("Project skipped...");
            return;
        }

        if (lombok.getGroupId() == null) {
            lombok.setGroupId(LOMBOK_GROUP_ID);
        }
        if (lombok.getArtifactId() == null) {
            lombok.setArtifactId(LOMBOK_ARTIFACT_ID);
        }

        Optional<Artifact> lombokOptional = getLombokArtifact();
        if (lombokOptional.isEmpty()) {
            LOGGER.warn("Lombok artifact was not found!");
            return;
        }

        File sourceDirectoryFile = new File(sourceDirectory);

        Artifact lombokArtifact = lombokOptional.get();
        File lombokJar = lombokArtifact.getFile();

        DelombokExecutor delombokExecutor = new DelombokExecutor(sourceDirectoryFile, lombokJar, outputDirectorySuffix);

        try {
            delombokExecutor.execute();
        } catch (DelombokExecutorException e) {
            throw new MojoExecutionException("Delombok execution failed!", e);
        }
    }

    private Optional<Artifact> getLombokArtifact() throws MojoExecutionException {
        LOGGER.info("Looking for lombok jar file! {}:{}", lombok.getGroupId(), lombok.getArtifactId());
        Optional<Artifact> lombokOptional = mavenProject.getArtifacts().stream().filter(artifact ->
                artifact.getArtifactId().equals(lombok.getArtifactId()) && artifact.getGroupId().equals(lombok.getGroupId())
        ).findFirst();

        if (lombokOptional.isEmpty() && failIfNotFound) {
            throw new MojoExecutionException("Failed to found Lombok jar file! Does you project really use Project Lombok?");
        }

        return lombokOptional;
    }
}
