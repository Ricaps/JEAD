package cz.muni.jena.codeminer.extractor;


public abstract class BaseCodeExtractor<T> implements CodeExtractor<T> {

    private final String identifier;

    protected BaseCodeExtractor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}
