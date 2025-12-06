package cz.muni.jena.issue.detectors.compilation_unit;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.MLDetectorConfig;

import java.util.List;
import java.util.function.Predicate;

public interface MachineLearningDetector {

    void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ExtractorDetectorsMapping params);
    void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate);

    /**
     * Data class containing list of ML detectors executed for one code extractor
     * @param extractor code extractor which handles preparation of data for detector
     * @param detectorConfigs list of detector configs which handles evaluation of extracted and inferred code by ML model
     * @param configuration JSON configuration of detector rules
     */
    record ExtractorDetectorsMapping(
            CodeExtractor<?> extractor,
            List<MLDetectorConfig> detectorConfigs,
            Configuration configuration
    ) {}
}
