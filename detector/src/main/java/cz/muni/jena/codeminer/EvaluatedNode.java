package cz.muni.jena.codeminer;

public interface EvaluatedNode {

    String getFullyQualifiedName();
    Integer getStartLine();
    String getContent();
}
