package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.model.EvaluationModel;

import java.util.List;

public interface OutputFormatter extends AutoCloseable {

    void add(List<? extends EvaluationModel> codeSnippets);

    void setOutputPath(String path);
}
