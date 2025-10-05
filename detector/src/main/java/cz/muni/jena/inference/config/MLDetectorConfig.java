package cz.muni.jena.inference.config;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.issue.IssueType;

import java.util.List;

public record MLDetectorConfig(
        String detectorName,
        CodeExtractor<?> extractor,
        String modelName,
        List<LabelEvaluationConfig> evaluations
) {

    public record LabelEvaluationConfig(
            String labelName,
            Double threshold,
            IssueType issueType
    ) {}
}
