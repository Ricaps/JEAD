package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.InferenceService;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.config.ModelConfiguration;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.IssueWithLazyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "true")
public class InferenceQueueHolderImpl implements InferenceQueueHolder<EvaluationModel>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceQueueHolderImpl.class);
    private final Map<String, InferenceQueue<EvaluationModel>> inferenceQueues = new ConcurrentHashMap<>();
    private final InferenceConfiguration inferenceConfiguration;
    private final InferenceService inferenceService;

    public InferenceQueueHolderImpl(InferenceConfiguration inferenceConfiguration, InferenceService inferenceService) {
        this.inferenceConfiguration = inferenceConfiguration;
        this.inferenceService = inferenceService;
    }

    @Override
    public void startQueues() {
        for (ModelConfiguration model : inferenceConfiguration.models()) {
            InferenceQueue<EvaluationModel> inferenceQueue = new InferenceQueue<>(model, inferenceService);
            inferenceQueues.put(model.modelName(), inferenceQueue);
            inferenceQueue.start();
        }
    }

    @Override
    public void addToQueue(String modelName, Stream<InferenceItem<EvaluationModel>> inferenceItemStream) {
        InferenceQueue<EvaluationModel> queue = inferenceQueues.get(modelName);
        if (queue == null) {
            LOGGER.warn("No queue defined for model {}", modelName);
            return;
        }

        queue.addToQueue(inferenceItemStream);
    }

    @Override
    public Stream<IssueWithLazyMeta> terminateQueuesAndWait() {
        return inferenceQueues.values()
                .stream()
                .flatMap(this::terminateQueueAndGet);
    }

    private Stream<IssueWithLazyMeta> terminateQueueAndGet(InferenceQueue<?> inferenceQueue) {
        try {
            return inferenceQueue.awaitTerminationAndGet(inferenceConfiguration.queueEndTimeout());
        } catch (InterruptedException e) {
            InferenceQueueHolderImpl.LOGGER.error("Failed to terminate inference queue for model {}. Results might be incomplete!", inferenceQueue.getModelName(), e);
            return Stream.of();
        }
    }

    @Override
    public void close() {
        terminateQueuesAndWait();
    }
}
