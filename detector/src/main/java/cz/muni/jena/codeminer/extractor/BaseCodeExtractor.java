package cz.muni.jena.codeminer.extractor;


abstract class BaseCodeExtractor implements CodeExtractor {

    private final String identifier;

    BaseCodeExtractor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }
}
