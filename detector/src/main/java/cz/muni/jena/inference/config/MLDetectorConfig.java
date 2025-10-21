package cz.muni.jena.inference.config;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.issue.IssueType;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public record MLDetectorConfig(
        @Size(min = 3, max = 20) String detectorName,
        @Nonnull CodeExtractor<?> extractor,
        @Nonnull ModelConfiguration model,
        @Nonnull @Size(min = 1) List<LabelEvaluationConfig> evaluations
) {

    public record LabelEvaluationConfig(
            @Nonnull String labelName,
            @Nonnull @Min(0) @Max(1) Double threshold,
            @Nonnull IssueType issueType
    ) {}
}
