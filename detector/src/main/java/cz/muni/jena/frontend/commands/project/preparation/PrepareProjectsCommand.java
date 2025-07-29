package cz.muni.jena.frontend.commands.project.preparation;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

@Command
public class PrepareProjectsCommand
{
    private static final String GRADLE_TASKS = """
            task copyDeps(type: Copy) {
                from(sourceSets.main.runtimeClasspath)
                into('target/dependency')
            }
                        
            task copyFlatDependencies(type: Copy) {
                into 'target/dependency'
                from {
                    subprojects.findAll { it.getSubprojects().isEmpty() }.
                            collect { it.configurations.runtimeClasspath }
                }
                duplicatesStrategy(DuplicatesStrategy.INCLUDE)
            }
            """;
    private static final String MAVEN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";
    private static final String OUTPUT_PATH_SUFFIX = File.separator + "target" + File.separator + "dependency";
    private static final String PREPARE_PROJECTS_DESCRIPTION = "Adds plugin to all Maven projects in directory and tasks" +
            " to all Gradle project in the directory.";
    private static final String DIRECTORY_PARAMETER_DESCRIPTION = "Absolute path to directory containing projects";

    @Command(command = "prepareProjects", description = PREPARE_PROJECTS_DESCRIPTION)
    public String prepareProjects(
            @Option(longNames = "directory", shortNames = 'd', required = true, description = DIRECTORY_PARAMETER_DESCRIPTION)
                    String directory
    )
    {
        Logger logger = LoggerFactory.getLogger(PrepareProjectsCommand.class);
        PomAndGradleFiles filesToVisit;
        try
        {
            filesToVisit = listFilesUsingFilesList(directory);
        } catch (IOException e)
        {
            return "Reading of projects in directory was unsuccessful.";
        }
        logger.atInfo().log("Jena will attempt to edit following files:" + System.lineSeparator() + Stream.concat(
                        filesToVisit.gradleFiles().stream(),
                        filesToVisit.mavenFiles().stream()
                )
                .collect(Collectors.joining(System.lineSeparator())));
        addTaskToGradleFiles(logger, filesToVisit);
        addPluginsToPomsMissingThem(logger, filesToVisit);
        return "The projects in directory: " + directory + "should now have the plugins and tasks needed.";
    }

    private void addPluginsToPomsMissingThem(Logger logger, PomAndGradleFiles filesToVisit)
    {
        for (String mavenFile : filesToVisit.mavenFiles())
        {
            try
            {
                Model mavenModel = parsePomXmlFileToMavenPomModel(mavenFile);
                Build build = Optional.ofNullable(mavenModel.getBuild())
                        .orElse(new Build());
                if (
                        build.getPlugins().stream()
                                .noneMatch(
                                        plugin -> MAVEN_PLUGIN_GROUP_ID.equals(plugin.getGroupId())
                                                && MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())
                                )
                )
                {
                    Plugin plugin = new Plugin();
                    plugin.setGroupId(MAVEN_PLUGIN_GROUP_ID);
                    plugin.setArtifactId(MAVEN_PLUGIN_ARTIFACT_ID);
                    plugin.setVersion("3.3.0");
                    PluginExecution copyDependenciesExecution = new PluginExecution();
                    copyDependenciesExecution.setId("copy-dependencies");
                    copyDependenciesExecution.setPhase("package");
                    copyDependenciesExecution.addGoal("copy-dependencies");
                    Xpp3Dom configuration = new Xpp3Dom("configuration");
                    Xpp3Dom outputDirectory = new Xpp3Dom("outputDirectory");
                    outputDirectory.setValue(
                            Paths.get(mavenFile)
                                    .getParent()
                                    .toAbsolutePath()
                                    .toString()
                                    .replace('/', File.separatorChar)
                                    .replace('\\', File.separatorChar)
                                    + OUTPUT_PATH_SUFFIX
                    );
                    configuration.addChild(outputDirectory);
                    copyDependenciesExecution.setConfiguration(configuration);
                    plugin.addExecution(copyDependenciesExecution);
                    build.addPlugin(plugin);
                    mavenModel.setBuild(build);
                    parseMavenPomModelToXmlString(mavenFile, mavenModel);
                    logger.atInfo().log("Plugin was added to: " + mavenFile);
                } else
                {
                    logger.atInfo().log(mavenFile + " already contains the plugin.");
                }
            } catch (XmlPullParserException e)
            {
                logger.atWarn().log("There was a mistake in parsing of this pom file: " + mavenFile);
            } catch (IOException e)
            {
                logger.atWarn().log("There was a mistake in reading of following file: " + mavenFile);
            }
        }
    }

    public void parseMavenPomModelToXmlString(String path, Model model) throws IOException
    {
        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
        try (Writer writer = new FileWriter(path))
        {
            mavenWriter.write(writer, model);
        }
    }

    public Model parsePomXmlFileToMavenPomModel(String path) throws XmlPullParserException, IOException
    {
        try (FileReader reader = new FileReader(path))
        {
            return new MavenXpp3Reader().read(reader);
        }
    }

    private void addTaskToGradleFiles(Logger logger, PomAndGradleFiles filesToVisit)
    {
        for (String gradleFile : filesToVisit.gradleFiles())
        {
            try
            {
                if (!FileUtils.readFileToString(new File(gradleFile), StandardCharsets.UTF_8).contains(GRADLE_TASKS))
                {
                    Files.write(
                            Paths.get(gradleFile),
                            GRADLE_TASKS.getBytes(),
                            StandardOpenOption.APPEND
                    );
                    logger.atInfo().log("Gradle tasks were added to file: " + gradleFile);
                } else
                {
                    logger.atInfo().log(gradleFile + " already contains the gradle tasks.");
                }
            } catch (IOException e)
            {
                logger.atWarn().log("Jena was unable to access file: " + gradleFile);
            }
        }
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
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
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
