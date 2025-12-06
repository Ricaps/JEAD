package cz.muni.jena.inference;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueCategory;
import cz.muni.jena.issue.IssueMetadataService;
import cz.muni.jena.issue.IssueWithLazyMeta;
import cz.muni.jena.issue.detectors.compilation_unit.EvaluationPredicate;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;
import cz.muni.jena.issue.detectors.machine_learning.InferenceQueueControl;
import cz.muni.jena.issue.detectors.machine_learning.InferenceQueueHolder;
import cz.muni.jena.parser.MachineLearningDetectorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class InferenceFacade implements InferenceQueueControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceFacade.class);

    private final InferenceService inferenceService;
    private final InferenceQueueHolder<EvaluationModel> inferenceQueueHolder;
    private final MachineLearningDetector machineLearningDetector;
    private final IssueMetadataService issueMetadataService;
    private final InferenceConfiguration inferenceConfiguration;

    @Inject
    public InferenceFacade(
            InferenceService inferenceService,
            InferenceQueueHolder<EvaluationModel> inferenceQueueHolder,
            MachineLearningDetector machineLearningDetector,
            IssueMetadataService issueMetadataService,
            InferenceConfiguration inferenceConfiguration) {
        this.inferenceService = inferenceService;
        this.inferenceQueueHolder = inferenceQueueHolder;
        this.machineLearningDetector = machineLearningDetector;
        this.issueMetadataService = issueMetadataService;
        this.inferenceConfiguration = inferenceConfiguration;
    }

    /**
     * Checks if the Inference server is online and ready
     *
     * @return true if inference server can be used, otherwise false
     */
    public boolean canUseMachineLearning() {
        return inferenceService.isServerReady();
    }

    @Override
    public void startQueues() {
        if (!canUseMachineLearning()) {
            LOGGER.warn("Inference server is not available. Machine learning detection will not be used!");
            return;
        }

        inferenceQueueHolder.startQueues();
    }

    @Override
    public Stream<IssueWithLazyMeta> terminateQueuesAndWait() {
        if (!canUseMachineLearning()) {
            LOGGER.warn("Inference server is not available. Machine learning detection was not used!");
            return Stream.of();
        }

        return inferenceQueueHolder.terminateQueuesAndWait();
    }

    public Optional<MachineLearningDetectorCallback> startMachineLearningEvaluation(boolean useMachineLearning, Set<IssueCategory> issueDetectorFilter, Configuration configuration) {
        if (!useMachineLearning) {
            return Optional.empty();
        }
        if (!canUseMachineLearning()) {
            LOGGER.warn("Inference server is not available. Machine learning evaluation won't be not used!");
            return Optional.empty();
        }
        EvaluationPredicate evaluationPredicate = issueDetectorFilter::contains;
        startQueues();

        return Optional.of(new MachineLearningDetectorCallback(machineLearningDetector, configuration, inferenceConfiguration, evaluationPredicate));
    }

    public List<Issue> endMachineLearningEvaluation(boolean useMachineLearning, String projectLabel) {
        if (!useMachineLearning || !canUseMachineLearning()) {
            return List.of();
        }
        List<IssueWithLazyMeta> foundIssues = terminateQueuesAndWait().toList();
        issueMetadataService.setMetaDataToIssues(foundIssues, projectLabel);

        return foundIssues.stream().map(IssueWithLazyMeta::issue).toList();
    }
}
