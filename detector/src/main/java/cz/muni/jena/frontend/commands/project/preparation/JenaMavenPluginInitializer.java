package cz.muni.jena.frontend.commands.project.preparation;

import cz.muni.jena.configuration.prepare_plugin.PreparePluginConfig;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JenaMavenPluginInitializer {

    public static final String SETTINGS_XML = "settings.xml";
    public static final String COPY_DEPENDENCIES_GOAL = "copy-dependencies";
    public static final String DELOMBOK_GOAL = "delombok";
    public static final String PACKAGE_PHASE = "package";
    private final Logger LOGGER = LoggerFactory.getLogger(JenaMavenPluginInitializer.class);
    private final PreparePluginConfig preparePluginConfig;

    public JenaMavenPluginInitializer(PreparePluginConfig preparePluginConfig) {
        this.preparePluginConfig = preparePluginConfig;
    }

    private void addExecution(String goal, Plugin plugin) {
        Optional<PluginExecution> executionOptional = getExistingExecution(plugin, goal);
        PluginExecution copyDependenciesExecution = executionOptional.orElse(new PluginExecution());
        copyDependenciesExecution.setId(goal);
        copyDependenciesExecution.setGoals(List.of(goal));
        copyDependenciesExecution.setPhase(PACKAGE_PHASE);

        if (executionOptional.isEmpty()) {
            plugin.addExecution(copyDependenciesExecution);
        }
    }

    /**
     * Adds maven-jena-plugin to the projects which are missing this plugin in their pom.xml definition
     *
     * @param filesToVisit files to check for missing maven-jena-plugin
     */
    public void addPluginsToPomsMissingThem(PomAndGradleFiles filesToVisit) {
        PreparePluginConfig.Artifact artifactConfig = preparePluginConfig.artifact();

        for (String mavenFile : filesToVisit.mavenFiles()) {
            try {
                Model mavenModel = parsePomXmlFileToMavenPomModel(mavenFile);

                addJenaPlugin(mavenFile, artifactConfig, mavenModel);
                addJenaPluginRepository(mavenFile, mavenModel);
                addSettingsFile(mavenFile);

                parseMavenPomModelToXmlString(mavenFile, mavenModel);
            } catch (XmlPullParserException e) {
                LOGGER.atWarn().log("There was a mistake in parsing of this pom file: {}", mavenFile);
            } catch (IOException e) {
                LOGGER.atWarn().log("There was a mistake in reading of following file: {}", mavenFile);
            }
        }
    }

    private void addJenaPlugin(String mavenFile, PreparePluginConfig.Artifact artifactConfig, Model mavenModel) {
        Build build = Optional.ofNullable(mavenModel.getBuild())
                .orElse(new Build());

        Optional<Plugin> pluginOptional = getExistingPlugin(build);
        pluginOptional.ifPresent(plugin -> LOGGER.atInfo().log("{} already contains the plugin.", mavenFile));

        Plugin plugin = pluginOptional.orElse(new Plugin());
        plugin.setGroupId(artifactConfig.groupId());
        plugin.setArtifactId(artifactConfig.artifactId());
        plugin.setVersion(artifactConfig.version());

        addExecution(COPY_DEPENDENCIES_GOAL, plugin);
        addExecution(DELOMBOK_GOAL, plugin);

        if (pluginOptional.isEmpty()) {
            build.addPlugin(plugin);
        }
        mavenModel.setBuild(build);

        LOGGER.atInfo().log("Plugin was added to: {}", mavenFile);
    }

    private void addJenaPluginRepository(String mavenFile, Model mavenModel) {
        PreparePluginConfig.Repository repositoryConfig = preparePluginConfig.repository();

        if (repositoryConfig == null) {
            LOGGER.atDebug().log("Repository configuration not defined, repository tag skipped.");
            return;
        }

        List<Repository> repositories = Optional.ofNullable(mavenModel.getPluginRepositories()).orElse(new ArrayList<>());

        Optional<Repository> repositoryOptional = getExistingRepository(repositories);
        repositoryOptional.ifPresent(repository -> LOGGER.atInfo().log("{} already contains the repository {}.", mavenFile, repository.getId()));

        Repository repository = repositoryOptional.orElse(new Repository());

        repository.setId(repositoryConfig.id());
        repository.setUrl(repositoryConfig.url());

        RepositoryPolicy repositoryPolicy = new RepositoryPolicy();
        repositoryPolicy.setEnabled(true);
        repository.setSnapshots(repositoryPolicy);
        repository.setReleases(repositoryPolicy);

        if (repositoryOptional.isEmpty()) {
            mavenModel.addPluginRepository(repository);
        }
    }

    private void addSettingsFile(String pomPath) throws XmlPullParserException, IOException {
        PreparePluginConfig.Repository repository = preparePluginConfig.repository();
        if (repository == null) {
            LOGGER.atDebug().log("Repository configuration not defined, skipping settings.xml generation.");
            return;
        }

        Path pom = Path.of(pomPath);
        Path settingsPath = pom.resolveSibling(SETTINGS_XML);
        Settings settingsModel = readOrCreateSettingsXml(settingsPath);

        List<Server> servers = Optional.ofNullable(settingsModel.getServers()).orElse(new ArrayList<>());

        Optional<Server> serverOptional = getExistingJenaServer(servers);
        serverOptional.ifPresent(server -> LOGGER.atInfo().log("settings.xml at '{}' already contains server {}", settingsPath, server.getId()));

        Server server = serverOptional.orElse(new Server());
        server.setId(repository.id());
        server.setUsername(repository.username());
        server.setPassword(repository.accessToken());

        if (serverOptional.isEmpty()) {
            servers.add(server);
            settingsModel.setServers(servers);
        }

        parseMavenSettingsModelToXmlString(settingsPath, settingsModel);

        LOGGER.atInfo().log("settings.xml file was added to: {}", settingsPath);
    }

    private Optional<PluginExecution> getExistingExecution(Plugin plugin, String executionId) {
        return plugin.getExecutions().stream().filter(pluginExecution -> pluginExecution.getId().equals(executionId)).findFirst();
    }

    private Optional<Plugin> getExistingPlugin(Build build) {
        return build.getPlugins().stream()
                .filter(
                        plugin -> preparePluginConfig.artifact().groupId().equals(plugin.getGroupId())
                                && preparePluginConfig.artifact().artifactId().equals(plugin.getArtifactId())
                ).findFirst();
    }

    private Optional<Server> getExistingJenaServer(List<Server> servers) {
        PreparePluginConfig.Repository repository = preparePluginConfig.repository();

        if (repository == null) {
            return Optional.empty();
        }

        return servers.stream()
                .filter(server -> repository.id().equals(server.getId()))
                .findFirst();
    }

    private Optional<Repository> getExistingRepository(List<Repository> repositories) {
        PreparePluginConfig.Repository repositoryConfig = preparePluginConfig.repository();

        if (repositoryConfig == null) {
            return Optional.empty();
        }

        return repositories.stream()
                .filter(repository -> repositoryConfig.id().equals(repository.getId()))
                .findFirst();
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

    private void parseMavenSettingsModelToXmlString(Path path, Settings model) throws IOException {
        SettingsXpp3Writer mavenWriter = new SettingsXpp3Writer();
        try (Writer writer = new FileWriter(path.toFile())) {
            mavenWriter.write(writer, model);
        }
    }

    private Settings readOrCreateSettingsXml(Path path) throws XmlPullParserException, IOException {
        File file = path.toFile();

        if (!file.exists()) {
            return new Settings();
        }

        try (FileReader reader = new FileReader(file)) {
            return new SettingsXpp3Reader().read(reader);
        }
    }
}
