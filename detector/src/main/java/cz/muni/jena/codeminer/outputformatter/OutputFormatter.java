package cz.muni.jena.codeminer.outputformatter;

import java.util.Collection;

public interface OutputFormatter extends AutoCloseable {

    void add(Collection<?> codeSnippets);

    void setOutputPath(String path);
}
