package cz.muni.jena.configuration.prepare_plugin;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the prepare plugin.
 * This record encapsulates settings related to Maven and Gradle plugins,
 * as well as repository details for fetching these plugins.
 *
 * @param mavenPlugin  The configuration for the Maven plugin artifact.
 * @param gradlePlugin The configuration for the Gradle plugin artifact.
 * @param repository   The configuration for the repository where plugins are located.
 */
@ConfigurationProperties(prefix = "prepare-plugin")
@Validated
public record PreparePluginConfig(@NotNull PreparePluginConfig.MavenPlugin mavenPlugin,
                                  @NotNull PreparePluginConfig.GradlePlugin gradlePlugin, @Nullable Repository repository) {

    /**
     * Represents an artifact with its group ID, artifact ID, and version.
     *
     * @param groupId    The group ID of the artifact.
     * @param artifactId The artifact ID of the artifact.
     * @param version    The version of the artifact.
     */
    public record MavenPlugin(@NotBlank String groupId, @NotBlank String artifactId, @NotBlank String version) {
    }

    /**
     * Represents a Gradle plugin with its group ID, artifact ID, version, and plugin ID.
     *
     * @param groupId    The group ID of the Gradle plugin.
     * @param artifactId The artifact ID of the Gradle plugin.
     * @param version    The version of the Gradle plugin.
     * @param pluginId   The ID of the Gradle plugin.
     */
    public record GradlePlugin(@NotBlank String groupId, @NotBlank String artifactId, @NotBlank String version,
                               @NotBlank String pluginId) {
    }

    /**
     * Represents a repository with its ID, URL, and optional authentication credentials.
     *
     * @param id       The ID of the repository.
     * @param url      The URL of the repository.
     * @param password The password for authentication, if required.
     * @param username The username for authentication, if required.
     */
    public record Repository(@NotBlank String id, @NotBlank String url, @Nullable String password,
                             @Nullable String username) {
    }
}
