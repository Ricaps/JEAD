package cz.muni.jena.issue.detectors.machine_learning;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "false", matchIfMissing = true)
public class EmptyMachineLearningDetector implements MachineLearningDetector {

    @Override
    public void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {

    }

    @Override
    public void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate) {

    }
}
