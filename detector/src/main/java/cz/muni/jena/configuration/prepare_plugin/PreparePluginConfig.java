package cz.muni.jena.configuration.prepare_plugin;

import jakarta.annotation.Nonnull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prepare-plugin")
public record PreparePluginConfig(@Nonnull String groupId, @Nonnull String artifactId, @Nonnull String version) {
}
