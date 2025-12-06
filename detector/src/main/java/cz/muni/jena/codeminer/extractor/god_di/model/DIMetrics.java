package cz.muni.jena.codeminer.extractor.god_di.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import cz.muni.jena.codeminer.extractor.god_di.EvaluationModelProvider;
import cz.muni.jena.inference.model.EvaluationModel;

public record DIMetrics(
        Integer linesOfCode,
        Long cyclomaticComplexity,
        Long injectedFields,
        Double lcom5,
        Integer methodsCount,
        Integer staticMethodsCount,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        EvaluationModelProvider evaluatedNode
) implements EvaluationModel {

    @Override
    public String getFullyQualifiedName() {
        return evaluatedNode.getFullyQualifiedName();
    }

    @Override
    public Integer getStartLine() {
        return evaluatedNode.getStartLine();
    }
}
