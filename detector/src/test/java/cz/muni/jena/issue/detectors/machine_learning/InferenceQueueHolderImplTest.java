package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.config.ModelConfiguration;
import cz.muni.jena.inference.InferenceService;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.IssueWithLazyMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InferenceQueueHolderImplTest {

    private static final int QUEUE_TIMEOUT_SECONDS = 1;

    private static InferenceConfiguration inferenceConfiguration(ModelConfiguration... models) {
        return new InferenceConfiguration(true, QUEUE_TIMEOUT_SECONDS, List.of(), List.of(models));
    }

    private static ModelConfiguration modelConfiguration(String modelName) {
        return new ModelConfiguration(5, 10_000, 100, 500, modelName);
    }

    @Test
    void startQueues_modelConfigured_createsQueuePerModel() {
        InferenceService inferenceService = mock(InferenceService.class);
        InferenceQueueHolderImpl holder = new InferenceQueueHolderImpl(
                inferenceConfiguration(
                        modelConfiguration("modelOne"),
                        modelConfiguration("modelTwo")
                ),
                inferenceService
        );

        holder.startQueues();

        assertThat(holder.inferenceQueues())
                .hasSize(2)
                .containsOnlyKeys("modelOne", "modelTwo");

        List<IssueWithLazyMeta> issues = holder.terminateQueuesAndWait().toList();
        assertThat(issues).isEmpty();
    }

    @Test
    void addToQueue_unknownModel_ignored() {
        InferenceService inferenceService = mock(InferenceService.class);
        InferenceQueueHolderImpl holder = new InferenceQueueHolderImpl(inferenceConfiguration(), inferenceService);

        holder.addToQueue("missingModel", Stream.of());

        verifyNoInteractions(inferenceService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void addToQueue_knownModel_itemProcessedOnTermination() {
        InferenceService inferenceService = mock(InferenceService.class);
        ModelConfiguration model = modelConfiguration("modelOne");
        InferenceQueueHolderImpl holder = new InferenceQueueHolderImpl(inferenceConfiguration(model), inferenceService);

        when(inferenceService.doInference(anyList(), eq(model.modelName()), eq(model.timeout())))
                .thenAnswer(invocation -> ((List<InferenceItem<EvaluationModel>>) invocation.getArgument(0)).stream());

        holder.startQueues();
        InferenceItem<EvaluationModel> item = new InferenceItem<>(new TestEvaluationModel(), ignored -> new IssueWithLazyMeta(null, null));

        holder.addToQueue(model.modelName(), Stream.of(item));
        List<IssueWithLazyMeta> issues = holder.terminateQueuesAndWait().toList();

        assertThat(issues).hasSize(1);
        verify(inferenceService, atLeastOnce()).doInference(anyList(), eq(model.modelName()), eq(model.timeout()));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void terminateQueuesAndWait_oneQueueInterrupted_returnsResultsFromOthers() throws InterruptedException {
        InferenceService inferenceService = mock(InferenceService.class);
        InferenceQueueHolderImpl holder = new InferenceQueueHolderImpl(inferenceConfiguration(), inferenceService);

        InferenceQueue<EvaluationModel> interruptedQueue = (InferenceQueue) mock(InferenceQueue.class);
        InferenceQueue<EvaluationModel> regularQueue = (InferenceQueue) mock(InferenceQueue.class);
        IssueWithLazyMeta expectedIssue = new IssueWithLazyMeta(null, null);

        when(interruptedQueue.getModelName()).thenReturn("brokenModel");
        when(interruptedQueue.awaitTerminationAndGet(anyInt())).thenThrow(new InterruptedException("interrupted"));
        when(regularQueue.awaitTerminationAndGet(anyInt())).thenReturn(Stream.of(expectedIssue));

        Map<String, InferenceQueue<EvaluationModel>> inferenceQueues = holder.inferenceQueues();
        inferenceQueues.put("brokenModel", interruptedQueue);
        inferenceQueues.put("healthyModel", regularQueue);

        List<IssueWithLazyMeta> issues = holder.terminateQueuesAndWait().toList();

        assertThat(issues).containsExactly(expectedIssue);
        verify(interruptedQueue).awaitTerminationAndGet(QUEUE_TIMEOUT_SECONDS);
        verify(regularQueue).awaitTerminationAndGet(QUEUE_TIMEOUT_SECONDS);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void terminateQueuesAndWait_queueExists_terminatesQueue() throws InterruptedException {
        InferenceService inferenceService = mock(InferenceService.class);
        InferenceQueueHolderImpl holder = new InferenceQueueHolderImpl(inferenceConfiguration(), inferenceService);

        InferenceQueue<EvaluationModel> queue = (InferenceQueue) mock(InferenceQueue.class);
        when(queue.awaitTerminationAndGet(anyInt())).thenReturn(Stream.empty());

        holder.inferenceQueues().put("modelOne", queue);

        List<IssueWithLazyMeta> issues = holder.terminateQueuesAndWait().toList();
        assertThat(issues).isEmpty();

        verify(queue).awaitTerminationAndGet(QUEUE_TIMEOUT_SECONDS);
    }

    private static class TestEvaluationModel implements EvaluationModel {
        @Override
        public String getFullyQualifiedName() {
            return "sample.Class";
        }

        @Override
        public Integer getStartLine() {
            return 1;
        }
    }

}