package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.inference.InferenceService;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.Issue;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class InferenceQueue<T extends EvaluatedNode> {

    private static final int RETRY_TIMEOUT = 100;
    private final Logger LOGGER = LoggerFactory.getLogger(InferenceQueue.class);
    private final BlockingQueue<InferenceItem<T>> sendQueue = new LinkedBlockingQueue<>(1000);
    private final List<Issue> results = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final InferenceService inferenceService;
    private final String modelName;

    public InferenceQueue(String modelName, InferenceService inferenceService) {
        this.modelName = modelName;
        this.inferenceService = inferenceService;
    }

    public void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        executorService.scheduleAtFixedRate(this::flush, 0, 200, TimeUnit.MILLISECONDS);
        LOGGER.info("Started InferenceQueue for model {}", modelName);
    }

    private void flush() {
        List<InferenceItem<T>> batch = new ArrayList<>();
        sendQueue.drainTo(batch, 30);

        Stream<InferenceItem<T>> inferenceResult = inferenceService.doInference(batch, modelName);
        List<Issue> issues = inferenceResult
                .map(inferenceItem -> inferenceItem.issueMappingFunction().mapToIssue(inferenceItem))
                .filter(Objects::nonNull)
                .toList();
        results.addAll(issues);
    }

    public Stream<Issue> awaitTerminationAndGet(int awaitTimeout) throws InterruptedException {
        if (!running.get()) {
            return Stream.of();
        }

        while (!sendQueue.isEmpty()) {
            LOGGER.info("Waiting for all items to be processed, model name {}", modelName);
            flush();
            Thread.sleep(RETRY_TIMEOUT);
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(awaitTimeout, TimeUnit.SECONDS);
        if (!terminated) {
            LOGGER.error("Inference queue thread was terminated before it could finish all evaluations! Model name: {}", modelName);
        }

        running.set(false);
        LOGGER.info("Ended InferenceQueue for model {}", modelName);
        return results.stream();
    }

    public void addToQueue(Stream<InferenceItem<T>> inferenceItemStream) {
        sendQueue.addAll(inferenceItemStream.toList());
    }

    public String getModelName() {
        return modelName;
    }
}
