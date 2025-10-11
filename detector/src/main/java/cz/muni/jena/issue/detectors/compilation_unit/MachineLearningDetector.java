package cz.muni.jena.issue.detectors.compilation_unit;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.MLDetectorConfig;

import java.util.function.Predicate;

public interface MachineLearningDetector {

    void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration);
    void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate);
}
