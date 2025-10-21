package cz.muni.jena.issue.detectors.machine_learning;

import cz.muni.jena.inference.InferenceService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class InferenceQueueHolderImplTest {

    @Mock
    private InferenceService inferenceService;

    @InjectMocks
    private InferenceQueueHolderImpl inferenceQueueHolder;

    @Test
    void addedToQueue_itemCorrectlyProcessed() {

    }

}