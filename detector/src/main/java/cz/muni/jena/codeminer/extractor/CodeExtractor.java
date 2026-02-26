package cz.muni.jena.codeminer.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.frontend.commands.commands.CommandSettingsMap;

import java.util.stream.Stream;

/**
 * Interface defining contract for code extractors.
 * Code extractors are responsible for extracting specific features from the code and transforming it into a format suitable for evaluation.
 *
 * @param <T> Type of the evaluation model that the extractor produces.
 */
public interface CodeExtractor<T extends EvaluationModel> {

    /**
     * Extracts features from the given class or interface declaration based on the provided configuration.
     * This method serves as a convenience method that calls the more detailed extract method with an empty CommandSettingsMap.
     * @param classOrInterface The class or interface declaration from which to extract features.
     * @param configuration The configuration that may influence the extraction process.
     * @return A stream of extracted evaluation models of type T.
     */
    default Stream<T> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration) {
        return extract(classOrInterface, configuration, new CommandSettingsHashMap());
    }

    /**
     * Extracts features from the given class or interface declaration based on the provided configuration and command settings.
     * @param classOrInterface The class or interface declaration from which to extract features.
     * @param configuration The configuration that may influence the extraction process.
     * @param commandSettingsMap A map of command settings that may influence the extraction process.
     * @return A stream of extracted evaluation models of type T.
     */
    Stream<T> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration, CommandSettingsMap commandSettingsMap);

    /**
     * Returns the unique identifier of this code extractor. This identifier is used to reference the extractor in configurations and command settings.
     * @return The unique identifier of this code extractor.
     */
    String getIdentifier();
}
