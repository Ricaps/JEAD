package cz.fi.muni.jena.delombok;


import cz.fi.muni.jena.model.ProjectModel;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
     * Output directory where the delomboked source codes should be stored
     */
    @Parameter(property = "jena.delombok.outputDirectory", defaultValue = "src-delombok")
    private String outputDirectory;

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

        LOGGER.info("Looking for lombok jar file! {}:{}", lombok.getGroupId(), lombok.getArtifactId());
        Optional<Artifact> lombokOptional = mavenProject.getArtifacts().stream().filter(artifact ->
                artifact.getArtifactId().equals(lombok.getArtifactId()) && artifact.getGroupId().equals(lombok.getGroupId())
        ).findFirst();

        if (lombokOptional.isEmpty() && failIfNotFound) {
            throw new MojoExecutionException("Failed to found Lombok jar file! Does you project really use Project Lombok?");
        } else if (lombokOptional.isEmpty()) {
            LOGGER.warn("Failed to found Lombok jar file!");
            return;
        }

        File sourceDirectoryFile = new File(sourceDirectory);
        if (!sourceDirectoryFile.isDirectory()) {
            throw new MojoExecutionException(String.format("The 'sourceDirectory' property %s is not a directory!", sourceDirectory));
        }

        Artifact lombokArtifact = lombokOptional.get();
        File lombokJar = lombokArtifact.getFile();

        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add(lombokJar.getAbsolutePath());
        commands.add("delombok");
        commands.add(sourceDirectory);
        commands.add("-d");
        commands.add(outputDirectory);

        File workingDirectory = sourceDirectoryFile.getParentFile();
        LOGGER.info("Starting delombok with command {}", String.join(" ", commands));
        LOGGER.info("At working directory {}", workingDirectory);

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(workingDirectory);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new MojoExecutionException("Delombok failed with status code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to run delombok!", e);
        }

        LOGGER.info("Delombok output written to {}", workingDirectory.toPath().resolve(outputDirectory));
    }
}
