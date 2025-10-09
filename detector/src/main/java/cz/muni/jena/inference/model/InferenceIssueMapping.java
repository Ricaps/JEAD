package cz.muni.jena.inference.model;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.issue.Issue;

@FunctionalInterface
public interface InferenceIssueMapping<T extends EvaluatedNode> {

    Issue mapToIssue(InferenceItem<T> inferenceItem);
}
