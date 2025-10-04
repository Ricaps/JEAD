package cz.muni.jena.codeminer.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;

import java.util.Collection;
import java.util.stream.Stream;

public interface CodeExtractor<T extends EvaluatedNode> {

    Stream<T> extract(ClassOrInterfaceDeclaration classOrInterface);
    String getIdentifier();
}
