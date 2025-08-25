package cz.muni.jena.codeminer.outputformatter;

import java.util.Collection;

public interface OutputFormatter extends AutoCloseable {

    void add(Collection<Object> codeSnippets);

    void setOutputPath(String path);
}
