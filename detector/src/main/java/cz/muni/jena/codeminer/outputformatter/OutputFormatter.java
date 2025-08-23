package cz.muni.jena.codeminer.outputformatter;

import java.util.List;

public interface OutputFormatter extends AutoCloseable {

    void add(List<Object> codeSnippets);

    void setOutputPath(String path);
}
