package cz.muni.jena.issue.detectors.machine_learning;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningIssueDetector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "false")
public class EmptyMachineLearningDetector implements MachineLearningIssueDetector {

    @Override
    public @NonNull Stream<Issue> findIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        return Stream.empty();
    }

    @Override
    public void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate) {

    }
}
