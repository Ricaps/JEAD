package cz.muni.jena.codeminer.extractor.multi_service.model;

import cz.muni.jena.inference.model.EvaluationModel;

import java.util.List;

public record MultiServiceMethods(List<Method> methods, Integer lcom4, String fullyQualifiedName, Integer startLine) implements EvaluationModel {
    @Override
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public Integer getStartLine() {
        return startLine;
    }

    public record Method(String name, String signature) {}
}
