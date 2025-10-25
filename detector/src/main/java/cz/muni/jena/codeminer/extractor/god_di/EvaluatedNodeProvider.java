package cz.muni.jena.codeminer.extractor.god_di;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.muni.jena.codeminer.EvaluatedNode;

public record EvaluatedNodeProvider(
        String fullyQualifiedName,
        Integer startLine
) implements EvaluatedNode {

    @Override
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public Integer getStartLine() {
        return startLine;
    }

    @Override
    @JsonIgnore
    public String getContent() {
        return null;
    }
}
