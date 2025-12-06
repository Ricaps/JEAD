package cz.muni.jena.issue.detectors.machine_learning;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.codeminer.extractor.CodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.AnalysisType;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueWithLazyMeta;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "true")
public class MachineLearningDetectorCombiner implements MachineLearningDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineLearningDetectorCombiner.class);
    private final InferenceConfiguration inferenceConfiguration;
    private Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate;
    private final InferenceQueueHolder<EvaluationModel> inferenceQueueHolder;

    @Inject
    public MachineLearningDetectorCombiner(InferenceConfiguration inferenceConfiguration, InferenceQueueHolder<EvaluationModel> inferenceQueueHolder) {
        this.inferenceConfiguration = inferenceConfiguration;
        this.inferenceQueueHolder = inferenceQueueHolder;
    }

    @Override
    public void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {

        inferenceConfiguration.detectors()
                .stream()
                .collect(groupingBy(MLDetectorConfig::extractor))
                .forEach((key, value) -> {
                    List<MLDetectorConfig> filteredDetectorConfigs = value.stream().filter((extractor) -> {
                        if (evaluationPredicate == null) {
                            return true;
                        }

                        return extractor.evaluations().stream().anyMatch(evaluation -> evaluationPredicate.test(evaluation));
                    }).toList();

                    if (filteredDetectorConfigs.isEmpty()) {
                        // Current extractor shouldn't be run for this configuration
                        return;
                    }

                    processExtractor(classOrInterfaceDeclaration, key, filteredDetectorConfigs, configuration);
                });
    }

    private void processExtractor(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, CodeExtractor<?> extractor, List<MLDetectorConfig> detectorConfigs, Configuration configuration) {
        Collection<? extends EvaluationModel> inferenceItems = extractor
                .extract(classOrInterfaceDeclaration, configuration)
                .toList();

        if (inferenceItems.isEmpty()) {
            return;
        }

        try {
            detectorConfigs
                    .forEach(detector -> processDetector(classOrInterfaceDeclaration, inferenceItems, detector));
        } catch (InferenceFailedException ex) {
            LOGGER.error("Inference for class {}. Reason: {}", classOrInterfaceDeclaration.getFullyQualifiedName(), ex.getMessage());
            LOGGER.trace("Stacktrace: ", ex);
        }
    }

    private void processDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Collection<? extends EvaluationModel> evaluatedNodesStream, MLDetectorConfig mlDetectorConfig) {
        Stream<InferenceItem<EvaluationModel>> inferenceItemStream = evaluatedNodesStream
                .stream()
                .map(evaluatedNode -> new InferenceItem<>(evaluatedNode, (inferenceItem) -> this.mapItemToIssue(classOrInterfaceDeclaration, inferenceItem, mlDetectorConfig)));

        inferenceQueueHolder.addToQueue(mlDetectorConfig.model().modelName(), inferenceItemStream);

    }

    private <T extends EvaluationModel> IssueWithLazyMeta mapItemToIssue(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, InferenceItem<T> inferenceItem, MLDetectorConfig mlDetectorConfig) {
        Optional<MLDetectorConfig.LabelEvaluationConfig> matchingEvaluation = getMatchingEvaluation(inferenceItem, mlDetectorConfig);

        return matchingEvaluation
                .map(evaluation -> new Issue(evaluation.issueType(), inferenceItem.getStartLine(), inferenceItem.getFullyQualifiedName(), AnalysisType.MACHINE_LEARNING))
                .map(issue -> new IssueWithLazyMeta(issue, classOrInterfaceDeclaration))
                .orElse(null);
    }

    private <T extends EvaluationModel> Optional<MLDetectorConfig.LabelEvaluationConfig> getMatchingEvaluation(InferenceItem<T> inferenceItem, MLDetectorConfig mlDetectorConfig) {
        return mlDetectorConfig.evaluations().stream()
                .filter(evaluation -> {
                    if (evaluationPredicate == null) {
                        return true;
                    }

                    return evaluationPredicate.test(evaluation);
                })
                .filter(evaluation ->
                        inferenceItem.labels().stream().anyMatch(label -> label.matches(evaluation.labelName(), evaluation.threshold()))
                ).findAny();
    }

    @Override
    public void setEvaluationPredicate(Predicate<MLDetectorConfig.LabelEvaluationConfig> evaluationPredicate) {
        this.evaluationPredicate = evaluationPredicate;
    }
}
