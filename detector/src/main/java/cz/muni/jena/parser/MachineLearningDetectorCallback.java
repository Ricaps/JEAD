package cz.muni.jena.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.issue.detectors.compilation_unit.EvaluationPredicate;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.groupingBy;

public record MachineLearningDetectorCallback(MachineLearningDetector machineLearningDetector,
                                              Configuration configuration,
                                              InferenceConfiguration inferenceConfiguration,
                                              EvaluationPredicate evaluationPredicate
) implements SourceRoot.Callback {

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {

        List<MachineLearningDetector.ExtractorDetectorsMapping> extractorDetectorsMapping
                = getExtractorDetectorsMapping();

        result.ifSuccessful((compilationUnit) -> process(compilationUnit, extractorDetectorsMapping
        ));

        return Result.DONT_SAVE;
    }

    /**
     * Groups all ML detectors by code extractor. Then for each code extractor, it creates a list of detectors,
     * which should be executed with particular code extracted and processed by the code extractor.
     * @return List of extractor -> detectors mappings
     */
    private List<MachineLearningDetector.ExtractorDetectorsMapping> getExtractorDetectorsMapping() {
        return inferenceConfiguration.detectors()
                .stream()
                .collect(groupingBy(MLDetectorConfig::extractor))
                .entrySet()
                .stream()
                .map((entry) -> {
                    CodeExtractor<?> extractor = entry.getKey();
                    List<MLDetectorConfig> detectorConfigsForExtractor = entry.getValue();

                    List<MLDetectorConfig> filteredDetectorConfigs = detectorConfigsForExtractor.stream().filter((detectorConfig) -> {
                        if (evaluationPredicate == null) {
                            return true;
                        }

                        // If evaluation predicate is defined, filter out detectors which shouldn't be run for this configuration
                        return detectorConfig.evaluations().stream().anyMatch(config -> evaluationPredicate.test(config.issueType().getCategory()));
                    }).toList();

                    if (filteredDetectorConfigs.isEmpty()) {
                        // Current extractor shouldn't be run for this configuration
                        return null;
                    }

                    return new MachineLearningDetector.ExtractorDetectorsMapping(extractor, filteredDetectorConfigs, configuration, evaluationPredicate);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private void process(CompilationUnit compilationUnit, List<MachineLearningDetector.ExtractorDetectorsMapping> extractorDetectorsMapping
    ) {
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                .forEach((classOrInterfaceDeclaration -> processNode(classOrInterfaceDeclaration, extractorDetectorsMapping
                )));
    }

    /**
     * Run all detectors associated with concrete code extractor. Run them for each class or interface declaration
     * @param classOrInterfaceDeclaration parsed class or interface
     * @param extractorDetectorsMapping mapping of code extractors to machine learning detectors
     */
    private void processNode(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, List<MachineLearningDetector.ExtractorDetectorsMapping> extractorDetectorsMapping
    ) {
        extractorDetectorsMapping
                .forEach(codeExtractorMapping -> machineLearningDetector.runDetector(classOrInterfaceDeclaration, codeExtractorMapping));

    }
}
