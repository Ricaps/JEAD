package cz.muni.jena.frontend.commands.project.preparation;

import cz.muni.jena.configuration.prepare_plugin.PreparePluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;

@Component
public class JenaGradlePluginInitializer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JenaGradlePluginInitializer.class);
    private static final String PREPARE_SCRIPTS_PATH = "classpath:prepareScripts/";
    private static final String JENA_GRADLE_FILENAME = "jena.gradle";
    private static final String REPOSITORY_FILENAME = "repository.gradle";
    private static final String CREDENTIALS_FILENAME = "credentials.gradle";

    private final ResourceLoader resourceLoader;
    private final PreparePluginConfig preparePluginConfig;
    private String gradleTasks;

    public JenaGradlePluginInitializer(
            PreparePluginConfig preparePluginConfig,
            ResourceLoader resourceLoader
    ) {
        this.preparePluginConfig = preparePluginConfig;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void afterPropertiesSet() {
        gradleTasks = getProcessedBuildScript();
    }

    void addTaskToGradleFiles(PomAndGradleFiles filesToVisit) {
        for (String gradleFile : filesToVisit.gradleFiles()) {
            Path buildGradlePath = Path.of(gradleFile);

            try {
                appendToBuildGradle(buildGradlePath);
            } catch (IOException e) {
                LOGGER.error("Jena was unable to access the file", e);
            }
        }
    }


    /**
     * Appends task to the build.gradle file if not already present.
     *
     * @param gradlePath gradle file path
     */
    private void appendToBuildGradle(Path gradlePath) throws IOException {
        String buildGradleContent = Files.readString(gradlePath);

        if (buildGradleContent.contains(preparePluginConfig.gradlePlugin().pluginId())) {
            LOGGER.info("{} already contains Jena plugin definition!", gradlePath);
            return;
        }

        Files.writeString(
                gradlePath,
                gradleTasks,
                StandardOpenOption.APPEND
        );
        LOGGER.atInfo().log("Gradle tasks were added to file: " + gradlePath);

    }

    private String getProcessedBuildScript() {
        PreparePluginConfig.GradlePlugin gradlePlugin = preparePluginConfig.gradlePlugin();

        String repositoryContent = getRepositoryContent().orElse("");

        Properties properties = new Properties();
        properties.setProperty("plugin.groupId", gradlePlugin.groupId());
        properties.setProperty("plugin.artifactId", gradlePlugin.artifactId());
        properties.setProperty("plugin.version", gradlePlugin.version());
        properties.setProperty("plugin.pluginId", gradlePlugin.pluginId());
        properties.setProperty("plugin.repository", repositoryContent);

        return getProcessedFileContent(JENA_GRADLE_FILENAME, properties);
    }

    private Optional<String> getRepositoryContent() {
        PreparePluginConfig.Repository repository = preparePluginConfig.repository();

        if (repository != null) {
            Properties properties = new Properties();
            String credentials = getCredentialsContent(repository).orElse("");
            properties.setProperty("repository.id", repository.id());
            properties.setProperty("repository.url", repository.url());
            properties.setProperty("repository.credentials", credentials);

            return Optional.of(getProcessedFileContent(REPOSITORY_FILENAME, properties));
        }
        return Optional.empty();
    }

    private Optional<String> getCredentialsContent(PreparePluginConfig.Repository repository) {
        Properties credentialsProps = new Properties();

        if (repository.username() != null && repository.password() != null) {
            credentialsProps.setProperty("repository.username", repository.username());
            credentialsProps.setProperty("repository.password", repository.password());
            return Optional.of(getProcessedFileContent(CREDENTIALS_FILENAME, credentialsProps));
        }
        return Optional.empty();
    }

    private String getProcessedFileContent(String fileName, Properties properties) {
        String fileContent = loadFile(fileName);

        PropertyPlaceholderHelper propertyHelper = new PropertyPlaceholderHelper("${", "}");
        return propertyHelper.replacePlaceholders(fileContent, properties);
    }

    private String loadFile(String fileName) {
        Resource resource = resourceLoader.getResource(PREPARE_SCRIPTS_PATH + fileName);

        if (!resource.exists()) {
            throw new IllegalStateException("Cannot gradle resource file '%s'".formatted(fileName));
        }

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read gradle resource file '%s'".formatted(fileName), e);
        }
    }
}
