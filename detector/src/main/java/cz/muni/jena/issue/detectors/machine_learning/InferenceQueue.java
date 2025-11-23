package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.InferenceService;
import cz.muni.jena.inference.config.ModelConfiguration;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.IssueWithLazyMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class InferenceQueue<T extends EvaluationModel> {

    private static final int RETRY_TIMEOUT = 100;
    private final Logger LOGGER = LoggerFactory.getLogger(InferenceQueue.class);
    private final BlockingQueue<InferenceItem<T>> sendQueue;
    private final List<IssueWithLazyMeta> results = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final InferenceService inferenceService;
    private final ModelConfiguration modelConfiguration;

    public InferenceQueue(ModelConfiguration modelConfiguration, InferenceService inferenceService) {
        this.modelConfiguration = modelConfiguration;
        this.inferenceService = inferenceService;
        this.sendQueue =  new LinkedBlockingQueue<>(modelConfiguration.queueSize());
    }

    public void start() {
        executorService.scheduleAtFixedRate(this::flush, 0, modelConfiguration.batchPeriod(), TimeUnit.MILLISECONDS);
        LOGGER.info("Started InferenceQueue for model {}", modelConfiguration.modelName());
    }

    private void flush() {
        flush(false);
    }

    private void flush(boolean runWithIncompleteBatch) {
        if (!runWithIncompleteBatch && sendQueue.size() < modelConfiguration.batchSize()) {
            return;
        }
        List<InferenceItem<T>> batch = new ArrayList<>();
        sendQueue.drainTo(batch, modelConfiguration.batchSize());

        Stream<InferenceItem<T>> inferenceResult = inferenceService.doInference(batch, modelConfiguration.modelName());
        List<IssueWithLazyMeta> issues = inferenceResult
                .map(inferenceItem -> inferenceItem.issueMappingFunction().mapToIssue(inferenceItem))
                .filter(Objects::nonNull)
                .toList();
        results.addAll(issues);
    }

    public Stream<IssueWithLazyMeta> awaitTerminationAndGet(int awaitTimeout) throws InterruptedException {
        executorService.shutdown();

        while (!sendQueue.isEmpty()) {
            LOGGER.info("Waiting for all items to be processed, model name {}", modelConfiguration.modelName());
            flush(true);
            Thread.sleep(RETRY_TIMEOUT);
        }

        boolean terminated = executorService.awaitTermination(awaitTimeout, TimeUnit.SECONDS);
        if (!terminated) {
            LOGGER.error("Inference queue thread was terminated before it could finish all evaluations! Model name: {}", modelConfiguration.modelName());
        }

        LOGGER.info("Ended InferenceQueue for model {}", modelConfiguration.modelName());
        return results.stream();
    }

    public void addToQueue(Stream<InferenceItem<T>> inferenceItemStream) {
        inferenceItemStream.forEach(this::putItemToQueue);
    }

    private void putItemToQueue(InferenceItem<T> inferenceItem) {
        try {
            sendQueue.put(inferenceItem);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to insert items into the queue! Model name: {}", modelConfiguration.modelName());
        }
    }

    public String getModelName() {
        return modelConfiguration.modelName();
    }
}
