package cz.muni.jena.inference.model;

import java.util.List;
import java.util.UUID;

public record InferenceItem<T extends EvaluationModel>(
        UUID id,
        T evaluableItem,
        List<Label> labels,
        InferenceIssueMapping<T> issueMappingFunction
) implements EvaluationModel {

    public InferenceItem(T evaluableItem, InferenceIssueMapping<T> issueMappingFunction) {
        this(UUID.randomUUID(), evaluableItem, List.of(), issueMappingFunction);
    }

    @Override
    public String getFullyQualifiedName() {
        return evaluableItem.getFullyQualifiedName();
    }

    @Override
    public Integer getStartLine() {
        return evaluableItem.getStartLine();
    }
}