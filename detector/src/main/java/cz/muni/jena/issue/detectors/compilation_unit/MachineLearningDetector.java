package cz.muni.jena.issue.detectors.compilation_unit;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.MLDetectorConfig;

import java.util.List;

public interface MachineLearningDetector {

    void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ExtractorDetectorsMapping params);

    /**
     * Data class containing list of ML detectors executed for one code extractor
     * @param extractor code extractor which handles preparation of data for detector
     * @param detectorConfigs list of detector configs which handles evaluation of extracted and inferred code by ML model
     * @param configuration JSON configuration of detector rules
     * @param evaluationPredicate used see {@link EvaluationPredicate} more information
     */
    record ExtractorDetectorsMapping(
            CodeExtractor<?> extractor,
            List<MLDetectorConfig> detectorConfigs,
            Configuration configuration,
            EvaluationPredicate evaluationPredicate
    ) {}
}
