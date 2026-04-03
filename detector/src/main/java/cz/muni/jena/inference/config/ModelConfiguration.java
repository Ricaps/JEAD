package cz.muni.jena.inference.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the inference model used by the detector.
 *
 * <p>Holds batching and model selection settings. Values are validated with Jakarta
 * Validation annotations: numeric properties must be positive and {@code modelName}
 * must be between 3 and 20 characters.</p>
 *
 * @param batchSize   Maximum number of items grouped into a single inference batch.
 *                    Must be a positive integer. Default: 5.
 * @param batchPeriod Time window (in milliseconds) to collect items into a batch
 *                    before sending for inference. Must be a positive integer.
 *                    Default: 200 ms.
 * @param queueSize   Maximum number of requests that may be queued waiting to be
 *                    batched. Must be a positive integer. Default: 1000.
 * @param timeout     Maximum time (in milliseconds) to wait for an inference
 *                    response before treating it as timed out. Must be a positive
 *                    integer. Default: 120000 ms (120 seconds).
 * @param modelName   Name or identifier of the model to use for inference. Must be
 *                    between 3 and 20 characters.
 */
@Validated
public record ModelConfiguration(
        @Positive @DefaultValue("5") int batchSize,
        @Positive @DefaultValue("200") int batchPeriod,
        @Positive @DefaultValue("1000") int queueSize,
        @Positive @DefaultValue("120000") int timeout,
        @Size(min = 3, max = 20) String modelName
) {
}
