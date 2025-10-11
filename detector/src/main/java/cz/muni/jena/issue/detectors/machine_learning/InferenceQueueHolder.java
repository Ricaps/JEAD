package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.inference.model.InferenceItem;
import cz.muni.jena.issue.Issue;

import java.util.stream.Stream;

public interface InferenceQueueHolder<T extends EvaluatedNode> extends InferenceQueueControl {

    void addToQueue(String modelName, Stream<InferenceItem<T>> inferenceItemStream);

}
