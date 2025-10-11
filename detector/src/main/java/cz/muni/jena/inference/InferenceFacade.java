package cz.muni.jena.inference;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.detectors.machine_learning.InferenceQueueControl;
import cz.muni.jena.issue.detectors.machine_learning.InferenceQueueHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.stream.Stream;

@Service
public class InferenceFacade implements InferenceQueueControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceFacade.class);

    private final InferenceService inferenceService;
    private final InferenceQueueHolder<EvaluatedNode> inferenceQueueHolder;

    @Inject
    public InferenceFacade(InferenceService inferenceService, InferenceQueueHolder<EvaluatedNode> inferenceQueueHolder) {
        this.inferenceService = inferenceService;
        this.inferenceQueueHolder = inferenceQueueHolder;
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
    public Stream<Issue> terminateQueuesAndWait() {
        if (!canUseMachineLearning()) {
            LOGGER.warn("Inference server is not available. Machine learning detection was not used!");
            return Stream.of();
        }

        return inferenceQueueHolder.terminateQueuesAndWait();
    }
}
