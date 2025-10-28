package cz.muni.jena.codeminer.extractor.god_di;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.muni.jena.codeminer.EvaluatedNode;

public record DIMetricsDto(
        Integer linesOfCode,
        Long cyclomaticComplexity,
        Long injectedFields,
        Integer lcom4,
        Integer methodsCount,
        @JsonIgnore String code,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) EvaluatedNodeProvider evaluatedNode
) implements EvaluatedNode {

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

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getContent() {
        return "";
    }
}
