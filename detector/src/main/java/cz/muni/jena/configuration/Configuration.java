package cz.muni.jena.configuration;

import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.configuration.mocking.MockingConfiguration;
import cz.muni.jena.configuration.persistence.PersistenceConfiguration;
import cz.muni.jena.configuration.security.SecurityConfiguration;
import cz.muni.jena.configuration.service_layer.ServiceLayerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public record Configuration(
        DIConfiguration diConfiguration,
        MockingConfiguration mockingConfiguration,
        SecurityConfiguration securityConfiguration,
        ServiceLayerConfiguration serviceLayerConfiguration,
        PersistenceConfiguration persistenceConfiguration
)
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    public static Optional<Configuration> readConfiguration(String path)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectReader configurationReader = objectMapper.readerFor(Configuration.class);
        try
        {
            return Optional.of(configurationReader.readValue(new File(path)));
        } catch (JacksonException e)
        {
            LOGGER.error("Failed to load configuration!", e);
            return Optional.empty();
        }
    }

    public static Configuration readConfiguration()
    {
        try
        {
            URL configurationFileURL = getConfigurationURL();
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectReader configurationReader = objectMapper.readerFor(Configuration.class);
            return configurationReader.readValue(Path.of(configurationFileURL.getPath()));
        } catch (JacksonException e)
        {
            throw new IllegalStateException(
                    "Configuration couldn't be loaded, please check if they are in correct format.", e
            );
        }
    }

    public static URL getConfigurationURL()
    {
        return Configuration.class.getResource("/configuration");
    }
}
