package cz.muni.jena.inference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@ConfigurationProperties(prefix = "inference")
public record InferenceConfiguration(
        boolean enabled,
        List<MLDetectorConfig> detectors
) {}
