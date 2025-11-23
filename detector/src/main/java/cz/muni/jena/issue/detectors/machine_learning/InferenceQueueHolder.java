package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.InferenceItem;

import java.util.stream.Stream;

public interface InferenceQueueHolder<T extends EvaluationModel> extends InferenceQueueControl {

    void addToQueue(String modelName, Stream<InferenceItem<T>> inferenceItemStream);

}
