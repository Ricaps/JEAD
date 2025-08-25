package cz.muni.jena.codeminer.extractor;


public abstract class BaseCodeExtractor implements CodeExtractor {

    private final String identifier;

    protected BaseCodeExtractor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}
