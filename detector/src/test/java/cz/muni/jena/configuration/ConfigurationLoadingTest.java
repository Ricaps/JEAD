package cz.muni.jena.configuration;

import cz.muni.jena.configuration.di.Annotation;
import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.configuration.mocking.ArgumentType;
import cz.muni.jena.configuration.mocking.MockingConfiguration;
import cz.muni.jena.configuration.mocking.MockingMethod;
import cz.muni.jena.configuration.persistence.PersistenceConfiguration;
import cz.muni.jena.configuration.security.EncryptionAlgorithm;
import cz.muni.jena.configuration.security.SecurityConfiguration;
import cz.muni.jena.configuration.security.TokenLifetimeSettings;
import cz.muni.jena.configuration.service_layer.ServiceLayerConfiguration;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cz.muni.jena.Preconditions.verifyCorrectWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;


class ConfigurationLoadingTest {
    private static String TEST_CONFIGURATION_PATH;

    @BeforeAll
    public static void setup() throws URISyntaxException {
        URL resourceUrl = ConfigurationLoadingTest.class.getResource("/testConfiguration");

        if (resourceUrl == null) {
            throw new IllegalStateException("Cannot find resource testConfiguration");
        }

        TEST_CONFIGURATION_PATH = Paths.get(resourceUrl.toURI()).toString();
    }

    @Test
    void jsonToAnnotationTest() {
        verifyCorrectWorkingDirectory();
        Optional<Configuration> configuration = TestConfigLoader.readConfiguration(TEST_CONFIGURATION_PATH);
        DIConfiguration diConfiguration = new DIConfiguration(
                List.of(
                        new Annotation("b.cd", false),
                        new Annotation("1.2", true)
                ), List.of(
                    new Annotation("cd.f", false),
                    new Annotation("5.2", true)
                ),
                10,
                6,
                List.of(
                        new Annotation("e.gfg"),
                        new Annotation("5.6")
                ),
                Set.of("abcd")
        );
        MockingConfiguration mockingConfiguration = new MockingConfiguration(
                Set.of("java.io.IOException"),
                List.of(new MockingMethod("efd", ArgumentType.CLASS, true))
        );
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                "user",
                "app",
                new TokenLifetimeSettings(
                        1L,
                        ChronoUnit.SECONDS
                ),
                List.of(
                        new EncryptionAlgorithm("name", 2, 5)
                ),
                Set.of("abc"),
                List.of("http")
        );
        assertThat(configuration)
                .isPresent()
                .contains(
                        new Configuration(
                                diConfiguration,
                                mockingConfiguration,
                                securityConfiguration,
                                new ServiceLayerConfiguration(2, 1, Set.of(new Annotation("cz.test.dfe"))),
                                new PersistenceConfiguration(Set.of("a.b.c"), "nPlusOne")
                        )
                );
    }
}
