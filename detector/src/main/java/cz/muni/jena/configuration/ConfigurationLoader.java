package cz.muni.jena.configuration;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

@Service
public class ConfigurationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);

    private static final String CLASSPATH_FILE_NAME = "configuration";
    private final ObjectMapper objectMapper;

    @Value("${configuration.path:file:./config/configuration.json}")
    private Resource externalizedConfig;

    @Value(ResourceLoader.CLASSPATH_URL_PREFIX + CLASSPATH_FILE_NAME)
    private Resource classpathConfig;

    public ConfigurationLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private Configuration readResource(Resource configResource) {
        try {
            if (!configResource.exists()) {
                throw new IllegalStateException("Configuration file not found in classpath: " + CLASSPATH_FILE_NAME);
            }

            ObjectReader configurationReader = objectMapper.readerFor(Configuration.class);
            try (InputStream inputStream = configResource.getInputStream()) {
                return configurationReader.readValue(inputStream);
            }
        } catch (JacksonException | IOException e) {
            throw new IllegalStateException("Configuration couldn't be loaded, please check if they are in correct format.", e);
        }
    }

    private Optional<Configuration> readExternal(String path) {
        ObjectReader configurationReader = objectMapper.readerFor(Configuration.class);
        try {
            return Optional.of(configurationReader.readValue(new File(path)));
        } catch (JacksonException e) {
            LOGGER.error("Failed to load configuration!", e);
            return Optional.empty();
        }
    }

    /**
     * Loads the configuration from the specified path or falls back to the default configuration if the path is null or invalid.
     * Default configuration is loaded either from the externalized config (./config/configuration.json) if it exists,
     * or from the classpath resource (resources/configuration).
     * <br>
     * The order of loading is as follows:
     * <ol>
     *      <li>If a non-null and non-blank path is provided, attempt to load the configuration from that path.</li>
     *      <li>If the provided path is null or blank, check if the externalized configuration file exists at ./config/configuration.json.</li>
     *      <li>If not found, load the default configuration from the classpath resource (resources/configuration).</li>
     * </ol>
     *
     * @param path the path to the external configuration file, can be null
     * @return the loaded Configuration object
     */
    public Configuration loadConfig(@Nullable String path) {
        if (path == null || path.isBlank()) {

            if (externalizedConfig.exists()) {
                LOGGER.info("Loading configuration from externalized config: {}", externalizedConfig.getFilename());
                return readResource(externalizedConfig);
            }

            LOGGER.info("No configuration path provided, loading default configuration from classpath.");
            return readResource(classpathConfig);
        } else {
            LOGGER.info("Loading configuration from external file: {}", path);
            return readExternal(path).orElseGet(() -> {
                LOGGER.warn("Failed to load external configuration, falling back to default configuration from classpath.");
                return readResource(classpathConfig);
            });
        }
    }

    private void copyResourceToFile(Resource resource, String targetPath) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            File targetFile = new File(targetPath);
            Files.copy(inputStream, targetFile.toPath());
        }
    }

    /**
     * Copies the default configuration file to the specified target path.
     * It copies externalized configuration if it exists (./config/configuration.json), otherwise it copies the classpath configuration (resources/configuration).
     *
     * @param targetPath the path where the configuration should be copied to
     * @throws IOException if an I/O error occurs during copying
     */
    public void copyConfiguration(String targetPath) throws IOException {
        if (externalizedConfig.exists()) {
            copyResourceToFile(externalizedConfig, targetPath);
            return;
        }

        copyResourceToFile(classpathConfig, targetPath);
    }
}
