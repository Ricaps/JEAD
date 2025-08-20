package cz.muni.jena.codeminer.outputformatter;

import java.util.List;

public class JsonOutput implements OutputFormatter {

    @Override
    public void saveInFormat(List<String> codeSnippets) {
        codeSnippets.forEach(System.out::println);
    }

}
