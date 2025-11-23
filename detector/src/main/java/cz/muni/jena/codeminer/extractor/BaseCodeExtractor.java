package cz.muni.jena.codeminer.extractor;


import cz.muni.jena.inference.model.EvaluationModel;

public abstract class BaseCodeExtractor<T extends EvaluationModel> implements CodeExtractor<T> {

    private final String identifier;

    protected BaseCodeExtractor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}
