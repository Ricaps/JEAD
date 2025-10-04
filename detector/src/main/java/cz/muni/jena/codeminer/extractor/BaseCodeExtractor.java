package cz.muni.jena.codeminer.extractor;


import cz.muni.jena.codeminer.EvaluatedNode;

public abstract class
BaseCodeExtractor<T extends EvaluatedNode> implements CodeExtractor<T> {

    private final String identifier;

    protected BaseCodeExtractor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}
