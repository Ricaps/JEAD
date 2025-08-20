package cz.muni.jena.codeminer.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;

public interface CodeExtractor {

    void extract(ClassOrInterfaceDeclaration classOrInterface, OutputFormatter codeSerializer);
    String getIdentifier();
}
