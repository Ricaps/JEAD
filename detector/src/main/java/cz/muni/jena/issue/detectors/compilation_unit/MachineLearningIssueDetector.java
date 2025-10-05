package cz.muni.jena.issue.detectors.compilation_unit;

import cz.muni.jena.inference.config.MLDetectorConfig;

import java.util.function.Predicate;

public interface MachineLearningIssueDetector extends IssueDetector {

    void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate);
}
