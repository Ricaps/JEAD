package cz.muni.jena.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ConfigurationLoaderTest {

    private static byte[] VALID_CONFIG_JSON;

    @BeforeAll
    static void loadConfigJson() throws IOException {
        try (InputStream is = ConfigurationLoaderTest.class.getResourceAsStream("/testConfiguration")) {
            if (is == null) {
                throw new IllegalStateException("Cannot find resource testConfiguration");
            }
            VALID_CONFIG_JSON = is.readAllBytes();
        }
    }

    private ConfigurationLoader configurationLoader;
    private Resource classpathConfig;
    private Resource externalizedConfig;

    @BeforeEach
    void setUp() {
        configurationLoader = new ConfigurationLoader(new ObjectMapper());
        classpathConfig = mock(Resource.class);
        externalizedConfig = mock(Resource.class);
        ReflectionTestUtils.setField(configurationLoader, "classpathConfig", classpathConfig);
        ReflectionTestUtils.setField(configurationLoader, "externalizedConfig", externalizedConfig);
    }

    @Test
    void loadsFromClasspathWhenNoPathAndNoExternalizedConfig() throws IOException {
        when(externalizedConfig.exists()).thenReturn(false);
        when(classpathConfig.exists()).thenReturn(true);
        when(classpathConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        Configuration result = configurationLoader.loadConfig(null);

        assertThat(result).isNotNull();
        verify(classpathConfig).getInputStream();
        verify(externalizedConfig, never()).getInputStream();
    }

    @Test
    void loadsFromClasspathWhenBlankPathAndNoExternalizedConfig() throws IOException {
        when(externalizedConfig.exists()).thenReturn(false);
        when(classpathConfig.exists()).thenReturn(true);
        when(classpathConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        Configuration result = configurationLoader.loadConfig("   ");

        assertThat(result).isNotNull();
        verify(classpathConfig).getInputStream();
    }

    @Test
    void throwsWhenNoPathAndClasspathConfigMissing() {
        when(externalizedConfig.exists()).thenReturn(false);
        when(classpathConfig.exists()).thenReturn(false);

        assertThatThrownBy(() -> configurationLoader.loadConfig(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuration file not found in classpath");
    }

    @Test
    void loadsFromExternalizedConfigWhenItExists() throws IOException {
        when(externalizedConfig.exists()).thenReturn(true);
        when(externalizedConfig.getFilename()).thenReturn("configuration.json");
        when(externalizedConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        Configuration result = configurationLoader.loadConfig(null);

        assertThat(result).isNotNull();
        verify(externalizedConfig).getInputStream();
        verify(classpathConfig, never()).getInputStream();
    }

    @Test
    void throwsWhenExternalizedConfigExistsButIsInvalid() throws IOException {
        when(externalizedConfig.exists()).thenReturn(true);
        when(externalizedConfig.getFilename()).thenReturn("configuration.json");
        when(externalizedConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream("not valid json {{{".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> configurationLoader.loadConfig(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuration couldn't be loaded");
    }

    @Test
    void loadsFromExplicitPathWhenProvided() throws Exception {
        Path tempFile = Files.createTempFile("configuration", ".json");
        Files.write(tempFile, VALID_CONFIG_JSON);

        try {
            Configuration result = configurationLoader.loadConfig(tempFile.toString());

            assertThat(result).isNotNull();
            verify(classpathConfig, never()).getInputStream();
            verify(externalizedConfig, never()).getInputStream();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void fallsBackToClasspathWhenExplicitPathIsInvalid() throws IOException {
        when(classpathConfig.exists()).thenReturn(true);
        when(classpathConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        Configuration result = configurationLoader.loadConfig("/nonexistent/path/configuration.json");

        assertThat(result).isNotNull();
        verify(classpathConfig).getInputStream();
    }

    @Test
    void fallsBackToClasspathWhenExplicitPathHasInvalidJson() throws Exception {
        Path tempFile = Files.createTempFile("configuration", ".json");
        Files.writeString(tempFile, "not valid json {{{");

        when(classpathConfig.exists()).thenReturn(true);
        when(classpathConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        try {
            Configuration result = configurationLoader.loadConfig(tempFile.toString());

            assertThat(result).isNotNull();
            verify(classpathConfig).getInputStream();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void loadedConfigurationHasCorrectContent() throws IOException {
        when(externalizedConfig.exists()).thenReturn(false);
        when(classpathConfig.exists()).thenReturn(true);
        when(classpathConfig.getInputStream())
                .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

        Configuration result = configurationLoader.loadConfig(null);

        assertThat(result.diConfiguration().maxNumberOfInjections()).isEqualTo(10);
        assertThat(result.securityConfiguration().sensitiveInformationRegex()).isEqualTo("user");
        assertThat(result.persistenceConfiguration().nPlusOneQueryRegex()).isEqualTo("nPlusOne");
    }

    @Test
    void copyConfigurationCopiesExternalizedConfigWhenItExists() throws IOException {
        Path targetFile = Files.createTempFile("copied-config", ".json");
        Files.deleteIfExists(targetFile);

        try {
            when(externalizedConfig.exists()).thenReturn(true);
            when(externalizedConfig.getInputStream())
                    .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

            configurationLoader.copyConfiguration(targetFile.toString());

            assertThat(targetFile).exists();
            assertThat(Files.readAllBytes(targetFile)).isEqualTo(VALID_CONFIG_JSON);
            verify(externalizedConfig).getInputStream();
            verify(classpathConfig, never()).getInputStream();
        } finally {
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    void copyConfigurationCopiesClasspathConfigWhenNoExternalizedConfig() throws IOException {
        Path targetFile = Files.createTempFile("copied-config", ".json");
        Files.deleteIfExists(targetFile);

        try {
            when(externalizedConfig.exists()).thenReturn(false);
            when(classpathConfig.getInputStream())
                    .thenReturn(new ByteArrayInputStream(VALID_CONFIG_JSON));

            configurationLoader.copyConfiguration(targetFile.toString());

            assertThat(targetFile).exists();
            assertThat(Files.readAllBytes(targetFile)).isEqualTo(VALID_CONFIG_JSON);
            verify(classpathConfig).getInputStream();
            verify(externalizedConfig, never()).getInputStream();
        } finally {
            Files.deleteIfExists(targetFile);
        }
    }
}
