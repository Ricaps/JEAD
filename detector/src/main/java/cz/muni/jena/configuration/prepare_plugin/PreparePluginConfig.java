package cz.muni.jena.configuration.prepare_plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prepare-plugin")
public record PreparePluginConfig(@Nonnull Artifact artifact, Repository repository) {

    public record Artifact(@Nonnull String groupId, @Nonnull String artifactId, @Nonnull String version) {
    }

    public record Repository(@Nonnull String id, @Nonnull String url, @Nullable String password,
                             @Nullable String username) {
    }
}
