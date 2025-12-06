package cz.muni.jena.inference.model;

import cz.muni.jena.issue.IssueWithLazyMeta;

@FunctionalInterface
public interface InferenceIssueMapping<T extends EvaluationModel> {

    IssueWithLazyMeta mapToIssue(InferenceItem<T> inferenceItem);
}
