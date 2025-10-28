package cz.muni.jena.codeminer.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.frontend.commands.commands.CommandSettingsMap;

import java.util.stream.Stream;

public interface CodeExtractor<T extends EvaluatedNode> {

    default Stream<T> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration) {
        return extract(classOrInterface, configuration, new CommandSettingsHashMap());
    }

    Stream<T> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration, CommandSettingsMap commandSettingsMap);
    String getIdentifier();
}
