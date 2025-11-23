package cz.muni.jena.codeminer.extractor.god_di;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.muni.jena.inference.model.EvaluationModel;

public record DIMetricsDto(
        Integer linesOfCode,
        Long cyclomaticComplexity,
        Long injectedFields,
        Integer lcom4,
        Integer methodsCount,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) EvaluationModelProvider evaluatedNode
) implements EvaluationModel {

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getFullyQualifiedName() {
        return evaluatedNode.getFullyQualifiedName();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getStartLine() {
        return evaluatedNode.getStartLine();
    }
}
