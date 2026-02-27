package cz.muni.jena.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ConfigurationLoader.class, ObjectMapperConfig.class})
class ConfigurationLoaderIntegrationTest {

    @Autowired
    private ConfigurationLoader configurationLoader;

    @Test
    void loadsClasspathConfigurationSuccessfully() {
        Configuration configuration = configurationLoader.loadConfig(null);

        assertThat(configuration).isNotNull();
        assertThat(configuration.diConfiguration()).isNotNull();
        assertThat(configuration.mockingConfiguration()).isNotNull();
        assertThat(configuration.securityConfiguration()).isNotNull();
        assertThat(configuration.serviceLayerConfiguration()).isNotNull();
        assertThat(configuration.persistenceConfiguration()).isNotNull();
    }

    @Test
    void classpathConfigurationHasExpectedValues() {
        Configuration configuration = configurationLoader.loadConfig(null);

        assertThat(configuration.diConfiguration().maxNumberOfInjections()).isPositive();
        assertThat(configuration.diConfiguration().injectionAnnotations()).isNotEmpty();
        assertThat(configuration.securityConfiguration().sensitiveInformationRegex()).isNotBlank();
        assertThat(configuration.persistenceConfiguration().nPlusOneQueryRegex()).isNotBlank();
    }
}
