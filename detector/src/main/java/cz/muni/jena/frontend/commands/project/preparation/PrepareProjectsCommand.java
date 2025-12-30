package cz.muni.jena.frontend.commands.project.preparation;

import cz.muni.jena.build_invokers.ProjectBuildService;
import jakarta.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private static final String PREPARE_PROJECTS_DESCRIPTION = "Adds plugin to all Maven projects in directory and tasks" +
            " to all Gradle project in the directory.";
    private static final String DIRECTORY_PARAMETER_DESCRIPTION = "Absolute path to directory containing projects";
    public static final String BUILD_PROJECTS_DESCRIPTION = "Automatically build target project to expose the dependencies";

    private final ProjectBuildService projectBuildService;
    private final JenaMavenPluginInitializer jenaMavenPluginInitializer;

    public PrepareProjectsCommand(ProjectBuildService projectBuildService, JenaMavenPluginInitializer jenaMavenPluginInitializer) {
        this.projectBuildService = projectBuildService;
        this.jenaMavenPluginInitializer = jenaMavenPluginInitializer;
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
        addTaskToGradleFiles(filesToVisit);
        jenaMavenPluginInitializer.addPluginsToPomsMissingThem(filesToVisit);

        if (buildProjects) {
            projectBuildService.runBuilds(Path.of(directory));
        }

        return "The projects in directory: " + directory + "should now have the plugins and tasks needed.";
    }

//    private void addPluginsToPomsMissingThem(PomAndGradleFiles filesToVisit)
//    {
//        for (String mavenFile : filesToVisit.mavenFiles())
//        {
//            try
//            {
//                Model mavenModel = parsePomXmlFileToMavenPomModel(mavenFile);
//                Build build = Optional.ofNullable(mavenModel.getBuild())
//                        .orElse(new Build());
//                if (
//                        build.getPlugins().stream()
//                                .noneMatch(
//                                        plugin -> MAVEN_PLUGIN_GROUP_ID.equals(plugin.getGroupId())
//                                                && MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())
//                                )
//                )
//                {
//                    Plugin plugin = new Plugin();
//                    plugin.setGroupId(preparePluginConfig.groupId());
//                    plugin.setArtifactId(preparePluginConfig.artifactId());
//                    plugin.setVersion(preparePluginConfig.version());
//                    PluginExecution copyDependenciesExecution = new PluginExecution();
//                    copyDependenciesExecution.setId("copy-dependencies");
//                    copyDependenciesExecution.setPhase("package");
//                    copyDependenciesExecution.addGoal("copy-dependencies");
//                    Xpp3Dom configuration = new Xpp3Dom("configuration");
//                    Xpp3Dom outputDirectory = new Xpp3Dom("outputDirectory");
//                    outputDirectory.setValue(
//                            Paths.get(mavenFile)
//                                    .getParent()
//                                    .toAbsolutePath()
//                                    .toString()
//                                    .replace('/', File.separatorChar)
//                                    .replace('\\', File.separatorChar)
//                                    + OUTPUT_PATH_SUFFIX
//                    );
//                    configuration.addChild(outputDirectory);
//                    copyDependenciesExecution.setConfiguration(configuration);
//                    plugin.addExecution(copyDependenciesExecution);
//                    build.addPlugin(plugin);
//                    mavenModel.setBuild(build);
//                    parseMavenPomModelToXmlString(mavenFile, mavenModel);
//                    LOGGER.atInfo().log("Plugin was added to: " + mavenFile);
//                } else
//                {
//                    LOGGER.atInfo().log(mavenFile + " already contains the plugin.");
//                }
//            } catch (XmlPullParserException e)
//            {
//                LOGGER.atWarn().log("There was a mistake in parsing of this pom file: " + mavenFile);
//            } catch (IOException e)
//            {
//                LOGGER.atWarn().log("There was a mistake in reading of following file: " + mavenFile);
//            }
//        }
//    }

//    public void parseMavenPomModelToXmlString(String path, Model model) throws IOException
//    {
//        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
//        try (Writer writer = new FileWriter(path))
//        {
//            mavenWriter.write(writer, model);
//        }
//    }
//
//    public Model parsePomXmlFileToMavenPomModel(String path) throws XmlPullParserException, IOException
//    {
//        try (FileReader reader = new FileReader(path))
//        {
//            return new MavenXpp3Reader().read(reader);
//        }
//    }

    private void addTaskToGradleFiles(PomAndGradleFiles filesToVisit)
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
                    LOGGER.atInfo().log("Gradle tasks were added to file: " + gradleFile);
                } else
                {
                    LOGGER.atInfo().log(gradleFile + " already contains the gradle tasks.");
                }
            } catch (IOException e)
            {
                LOGGER.atWarn().log("Jena was unable to access file: " + gradleFile);
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
