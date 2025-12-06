package cz.muni.jena.issue.detectors.machine_learning;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.exception.InferenceFailedException;
import cz.muni.jena.inference.config.MLDetectorConfig;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.AnalysisType;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueWithLazyMeta;
import cz.muni.jena.issue.detectors.compilation_unit.EvaluationPredicate;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "true")
public class MachineLearningDetectorCombiner implements MachineLearningDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineLearningDetectorCombiner.class);
    private final InferenceQueueHolder<EvaluationModel> inferenceQueueHolder;

    @Inject
    public MachineLearningDetectorCombiner(InferenceQueueHolder<EvaluationModel> inferenceQueueHolder) {
        this.inferenceQueueHolder = inferenceQueueHolder;
    }

    @Override
    public void runDetector(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ExtractorDetectorsMapping extractorDetectorsMapping) {
        Collection<? extends EvaluationModel> inferenceItems = extractorDetectorsMapping.extractor()
                .extract(classOrInterfaceDeclaration, extractorDetectorsMapping.configuration())
                .toList();

        if (inferenceItems.isEmpty()) {
            return;
        }

        try {
            extractorDetectorsMapping.detectorConfigs()
                    .forEach(detector -> processDetector(classOrInterfaceDeclaration, inferenceItems, detector, extractorDetectorsMapping.evaluationPredicate()));
        } catch (InferenceFailedException ex) {
            LOGGER.error("Inference for class {}. Reason: {}", classOrInterfaceDeclaration.getFullyQualifiedName(), ex.getMessage());
            LOGGER.trace("Stacktrace: ", ex);
        }
    }

    private void processDetector(
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            Collection<? extends EvaluationModel> evaluatedNodesStream,
            MLDetectorConfig mlDetectorConfig,
            EvaluationPredicate evaluationPredicate) {
        Stream<InferenceItem<EvaluationModel>> inferenceItemStream = evaluatedNodesStream
                .stream()
                .map(evaluatedNode -> new InferenceItem<>(
                        evaluatedNode,
                        (inferenceItem) -> this.mapItemToIssue(classOrInterfaceDeclaration, inferenceItem, mlDetectorConfig, evaluationPredicate))
                );

        inferenceQueueHolder.addToQueue(mlDetectorConfig.model().modelName(), inferenceItemStream);

    }

    private <T extends EvaluationModel> IssueWithLazyMeta mapItemToIssue(
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            InferenceItem<T> inferenceItem,
            MLDetectorConfig mlDetectorConfig,
            EvaluationPredicate evaluationPredicate
    ) {
        Optional<MLDetectorConfig.LabelEvaluationConfig> matchingEvaluation = getMatchingEvaluation(inferenceItem, mlDetectorConfig, evaluationPredicate);

        return matchingEvaluation
                .map(evaluation -> new Issue(evaluation.issueType(), inferenceItem.getStartLine(), inferenceItem.getFullyQualifiedName(), AnalysisType.MACHINE_LEARNING))
                .map(issue -> new IssueWithLazyMeta(issue, classOrInterfaceDeclaration))
                .orElse(null);
    }

    private <T extends EvaluationModel> Optional<MLDetectorConfig.LabelEvaluationConfig> getMatchingEvaluation(
            InferenceItem<T> inferenceItem,
            MLDetectorConfig mlDetectorConfig,
            EvaluationPredicate evaluationPredicate
    ) {
        return mlDetectorConfig.evaluations().stream()
                .filter(evaluation -> {
                    if (evaluationPredicate == null) {
                        return true;
                    }

                    return evaluationPredicate.test(evaluation.issueType().getCategory());
                })
                .filter(evaluation ->
                        inferenceItem.labels().stream().anyMatch(label -> label.matches(evaluation.labelName(), evaluation.threshold()))
                ).findAny();
    }
}
