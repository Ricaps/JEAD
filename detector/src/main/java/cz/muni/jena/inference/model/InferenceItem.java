package cz.muni.jena.inference.model;

import cz.muni.jena.codeminer.EvaluatedNode;

import java.util.List;
import java.util.UUID;

public record InferenceItem<T extends EvaluatedNode>(
        UUID id,
        T evaluableItem,
        List<Label> labels
) implements EvaluatedNode {

    @Override
    public String getFullyQualifiedName() {
        return evaluableItem.getFullyQualifiedName();
    }

    @Override
    public Integer getStartLine() {
        return evaluableItem.getStartLine();
    }

    @Override
    public String getContent() {
        return evaluableItem.getContent();

    }
}