package cz.muni.jena.inference.model;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.issue.IssueWithLazyMeta;

@FunctionalInterface
public interface InferenceIssueMapping<T extends EvaluatedNode> {

    IssueWithLazyMeta mapToIssue(InferenceItem<T> inferenceItem);
}
