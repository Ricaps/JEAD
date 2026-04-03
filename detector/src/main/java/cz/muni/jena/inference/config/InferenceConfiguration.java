package cz.muni.jena.inference.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for inference services and model orchestration.
 *
 * @param enabled whether inference integration is enabled in the application
 * @param queueTerminationTimeout seconds to wait for the end-of-queue marker before timing out
 * @param detectors list of ML detector configurations available for inference
 * @param models list of model configurations used by the detectors
 */
@ConfigurationProperties(prefix = "inference")
@Validated
public record InferenceConfiguration(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("20") @Positive int queueTerminationTimeout,
        List<MLDetectorConfig> detectors,
        List<ModelConfiguration> models
) {}
