package cz.muni.jena.codeminer.outputformatter;

import java.util.List;

public interface OutputFormatter {

    void saveInFormat(List<String> codeSnippets);
}
