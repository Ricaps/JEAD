package cz.muni.jena.inference.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
public record ModelConfiguration(
        @Positive @DefaultValue("5") int batchSize,
        @Positive @DefaultValue("200") int batchPeriod,
        @Positive @DefaultValue("1000") int queueSize,
        @Size(min = 3, max = 20) String modelName
) {
}
