package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.IssueWithLazyMeta;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = "inference.enabled", havingValue = "false", matchIfMissing = true)
public class EmptyInferenceQueueHolder implements InferenceQueueHolder<EvaluationModel> {

    @Override
    public void startQueues() {

    }

    @Override
    public void addToQueue(String modelName, Stream<InferenceItem<EvaluationModel>> inferenceItemStream) {

    }

    @Override
    public Stream<IssueWithLazyMeta> terminateQueuesAndWait() {
        return Stream.empty();
    }
}
