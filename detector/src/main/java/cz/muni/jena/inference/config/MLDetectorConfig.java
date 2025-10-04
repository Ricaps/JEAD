package cz.muni.jena.inference.config;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.issue.IssueType;

public record MLDetectorConfig(
        String detectorName,
        CodeExtractor<?> extractor,
        String modelName,
        IssueType issueType,
        LabelEvaluationConfig label
) {

    public record LabelEvaluationConfig(
            String labelName,
            Double threshold
    ) {}
}
