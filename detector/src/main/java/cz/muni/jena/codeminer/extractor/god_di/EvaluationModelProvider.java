package cz.muni.jena.codeminer.extractor.god_di;

import cz.muni.jena.inference.model.EvaluationModel;

public record EvaluationModelProvider(
        String fullyQualifiedName,
        Integer startLine
) implements EvaluationModel {

    @Override
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public Integer getStartLine() {
        return startLine;
    }
}
