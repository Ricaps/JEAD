package cz.muni.jena.frontend.commands.project.preparation;

import cz.muni.jena.build_invokers.ProjectBuildService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

@Command
public class PrepareProjectsCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareProjectsCommand.class);
    private static final String PREPARE_PROJECTS_DESCRIPTION = "Adds plugin to all Maven projects in directory and tasks" +
            " to all Gradle project in the directory.";
    private static final String DIRECTORY_PARAMETER_DESCRIPTION = "Absolute path to directory containing projects";
    public static final String BUILD_PROJECTS_DESCRIPTION = "Automatically build target project to expose the dependencies";

    private final ProjectBuildService projectBuildService;
    private final JeadMavenPluginInitializer jeadMavenPluginInitializer;
    private final JeadGradlePluginInitializer jeadGradlePluginInitializer;

    public PrepareProjectsCommand(ProjectBuildService projectBuildService, JeadMavenPluginInitializer jeadMavenPluginInitializer, JeadGradlePluginInitializer jeadGradlePluginInitializer) {
        this.projectBuildService = projectBuildService;
        this.jeadMavenPluginInitializer = jeadMavenPluginInitializer;
        this.jeadGradlePluginInitializer = jeadGradlePluginInitializer;
    }

    @Command(command = "prepareProjects", description = PREPARE_PROJECTS_DESCRIPTION)
    public String prepareProjects(
            @Option(longNames = "directory", shortNames = 'd', required = true, description = DIRECTORY_PARAMETER_DESCRIPTION)
                    String directory,
            @Option(longNames = "build", shortNames = 'b', defaultValue = "false", description = BUILD_PROJECTS_DESCRIPTION) Boolean buildProjects
    )
    {
        PomAndGradleFiles filesToVisit;
        try
        {
            filesToVisit = listFilesUsingFilesList(directory);
        } catch (IOException e)
        {
            return "Reading of projects in directory was unsuccessful.";
        }

        LOGGER.atInfo().log("Jena will attempt to edit following files:" + System.lineSeparator() + Stream.concat(
                        filesToVisit.gradleFiles().stream(),
                        filesToVisit.mavenFiles().stream()
                )
                .collect(Collectors.joining(System.lineSeparator())));
        jeadGradlePluginInitializer.addTaskToGradleFiles(filesToVisit);
        jeadMavenPluginInitializer.addPluginsToPomsMissingThem(filesToVisit);

        if (buildProjects) {
            projectBuildService.runBuilds(Path.of(directory));
        }

        return "The projects in directory: " + directory + "should now have the plugins and tasks needed.";
    }

    private PomAndGradleFiles listFilesUsingFilesList(String dir) throws IOException
    {
        PomAndGradleFiles filesToVisit = new PomAndGradleFiles(
                new ArrayList<>(),
                new ArrayList<>()
        );
        Files.walkFileTree(Paths.get(dir), Set.of(FOLLOW_LINKS), 2, new SimpleFileVisitor<>()
        {
            @Override
            @Nonnull
            public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs)
            {
                if (!Files.isDirectory(file))
                {
                    if (file.getFileName().toString().endsWith("pom.xml"))
                    {
                        filesToVisit.mavenFiles().add(file.toAbsolutePath().toString());
                    }
                    if (file.getFileName().toString().endsWith("build.gradle"))
                    {
                        filesToVisit.gradleFiles().add(file.toAbsolutePath().toString());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return filesToVisit;
    }
}
