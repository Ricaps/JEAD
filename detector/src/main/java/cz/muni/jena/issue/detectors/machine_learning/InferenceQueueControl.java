package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.issue.IssueWithLazyMeta;

import java.util.stream.Stream;

public interface InferenceQueueControl {

    /**
     * Starts inference queues, which handles sending of extracted code snippets to the inference server. <br>
     * One queue for each model is started.
     */
    void startQueues();

    /**
     * Terminates inference queues after the processing is done. <br>
     * Waits until all extracted snippets are processed.
     * @return stream of found issues
     */
    Stream<IssueWithLazyMeta> terminateQueuesAndWait();
}
