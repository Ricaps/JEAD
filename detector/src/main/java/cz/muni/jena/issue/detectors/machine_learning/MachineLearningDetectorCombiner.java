package cz.muni.jena.issue.detectors.machine_learning;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.inference.InferenceService;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningIssueDetector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "true")
public class MachineLearningDetectorCombiner implements MachineLearningIssueDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineLearningDetectorCombiner.class);
    private final InferenceConfiguration inferenceConfiguration;
    private final InferenceService inferenceService;
    private Predicate<MLDetectorConfig> detectorPredicate;

    @Inject
    public MachineLearningDetectorCombiner(InferenceConfiguration inferenceConfiguration, InferenceService inferenceService) {
        this.inferenceConfiguration = inferenceConfiguration;
        this.inferenceService = inferenceService;
    }

    @Override
    public @NonNull Stream<Issue> findIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {

        return inferenceConfiguration.detectors()
                .stream()
                .collect(groupingBy(MLDetectorConfig::extractor))
                .entrySet()
                .stream()
                .flatMap((entry) -> {
                    List<MLDetectorConfig> filteredDetectorConfigs = entry.getValue().stream().filter((extractor) -> {
                        if (detectorPredicate == null) {
                            return true;
                        }

                        return detectorPredicate.test(extractor);
                    }).toList();

                    return processExtractor(classOrInterfaceDeclaration, entry.getKey(), filteredDetectorConfigs);
                });
    }

    private Stream<Issue> processExtractor(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, CodeExtractor<?> extractor, List<MLDetectorConfig> detectorConfigs) {
        Collection<InferenceItem<EvaluatedNode>> inferenceItems = extractor
                .extract(classOrInterfaceDeclaration)
                .map(InferenceItem<EvaluatedNode>::new)
                .toList();

        if (inferenceItems.isEmpty()) {
            return Stream.of();
        }

        try {
            return detectorConfigs.stream()
                    .flatMap(detector -> processDetector(inferenceItems, detector));
        } catch (InferenceFailedException ex) {
            LOGGER.error("Inference for class {}. Reason: {}", classOrInterfaceDeclaration.getFullyQualifiedName(), ex.getMessage());
            LOGGER.trace("Stacktrace: ", ex);
        }

        return Stream.of();
    }

    private Stream<Issue> processDetector(Collection<InferenceItem<EvaluatedNode>> inferenceItemStream, MLDetectorConfig mlDetectorConfig) {

        try {
            return inferenceService.doInference(inferenceItemStream, mlDetectorConfig.modelName())
                    .filter(inferenceItem -> matchesAnyLabel(inferenceItem, mlDetectorConfig.label()))
                    .map(inferenceItem -> mapItemToIssue(inferenceItem, mlDetectorConfig.issueType()));
        } catch (InferenceFailedException ex) {
            throw new InferenceFailedException("Failed to evaluate detector %s ".formatted(mlDetectorConfig.detectorName()), ex);
        }
    }

    private boolean matchesAnyLabel(InferenceItem<EvaluatedNode> inferenceItem, MLDetectorConfig.LabelEvaluationConfig labelConfig) {
        return inferenceItem.labels().stream().anyMatch(label -> label.matches(labelConfig.labelName(), labelConfig.threshold()));
    }

    private Issue mapItemToIssue(InferenceItem<EvaluatedNode> inferenceItem, IssueType issueType) {
        return new Issue(issueType, inferenceItem.getStartLine(), inferenceItem.getFullyQualifiedName());
    }

    @Override
    public void setDetectorPredicate(Predicate<MLDetectorConfig> detectorPredicate) {
        this.detectorPredicate = detectorPredicate;
    }
}
