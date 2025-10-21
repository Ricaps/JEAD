package cz.muni.jena.inference.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@ConfigurationProperties(prefix = "inference")
@Validated
public record InferenceConfiguration(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("20") @Positive int queueEndTimeout,
        List<MLDetectorConfig> detectors,
        List<ModelConfiguration> models
) {}
