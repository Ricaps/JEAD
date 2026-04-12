package cz.muni.jena.codeminer.outputformatter;

@FunctionalInterface
public interface OutputFormatterInstanceProvider {

    OutputFormatter createInstance();
}
