package cz.muni.jena.frontend.commands.project.preparation;

import cz.muni.jena.configuration.prepare_plugin.PreparePluginConfig;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

@Component
public class JenaMavenPluginInitializer {

    public static final String COPY_DEPENDENCIES_GOAL = "copy-dependencies";
    public static final String DELOMBOK_GOAL = "delombok";
    public static final String PACKAGE_PHASE = "package";
    private final Logger LOGGER = LoggerFactory.getLogger(JenaMavenPluginInitializer.class);

    private final PreparePluginConfig preparePluginConfig;

    public JenaMavenPluginInitializer(PreparePluginConfig preparePluginConfig) {
        this.preparePluginConfig = preparePluginConfig;
    }

    private static void addExecution(String copyDependenciesGoal, Plugin plugin) {
        PluginExecution copyDependenciesExecution = new PluginExecution();
        copyDependenciesExecution.setId(copyDependenciesGoal);
        copyDependenciesExecution.addGoal(copyDependenciesGoal);
        copyDependenciesExecution.setPhase(PACKAGE_PHASE);
        plugin.addExecution(copyDependenciesExecution);
    }

    /**
     * Adds maven-jena-plugin to the projects which are missing this plugin in their pom.xml definition
     *
     * @param filesToVisit files to check for missing maven-jena-plugin
     */
    public void addPluginsToPomsMissingThem(PomAndGradleFiles filesToVisit) {
        for (String mavenFile : filesToVisit.mavenFiles()) {
            try {
                Model mavenModel = parsePomXmlFileToMavenPomModel(mavenFile);
                Build build = Optional.ofNullable(mavenModel.getBuild())
                        .orElse(new Build());

                if (containsPlugin(build)) {
                    LOGGER.atInfo().log("{} already contains the plugin.", mavenFile);
                    return;
                }

                Plugin plugin = new Plugin();
                plugin.setGroupId(preparePluginConfig.groupId());
                plugin.setArtifactId(preparePluginConfig.artifactId());
                plugin.setVersion(preparePluginConfig.version());

                addExecution(COPY_DEPENDENCIES_GOAL, plugin);
                addExecution(DELOMBOK_GOAL, plugin);

                build.addPlugin(plugin);

                mavenModel.setBuild(build);
                parseMavenPomModelToXmlString(mavenFile, mavenModel);

                LOGGER.atInfo().log("Plugin was added to: {}", mavenFile);
            } catch (XmlPullParserException e) {
                LOGGER.atWarn().log("There was a mistake in parsing of this pom file: {}", mavenFile);
            } catch (IOException e) {
                LOGGER.atWarn().log("There was a mistake in reading of following file: {}", mavenFile);
            }
        }
    }

    private boolean containsPlugin(Build build) {
        return build.getPlugins().stream()
                .anyMatch(
                        plugin -> preparePluginConfig.groupId().equals(plugin.getGroupId())
                                && preparePluginConfig.artifactId().equals(plugin.getArtifactId())
                );
    }

    private void parseMavenPomModelToXmlString(String path, Model model) throws IOException {
        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
        try (Writer writer = new FileWriter(path)) {
            mavenWriter.write(writer, model);
        }
    }

    private Model parsePomXmlFileToMavenPomModel(String path) throws XmlPullParserException, IOException {
        try (FileReader reader = new FileReader(path)) {
            return new MavenXpp3Reader().read(reader);
        }
    }
}
