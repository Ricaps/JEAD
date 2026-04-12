package cz.muni.jena.inference;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.config.InferenceConfiguration;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueCategory;
import cz.muni.jena.issue.IssueMetadataService;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.IssueWithLazyMeta;
import cz.muni.jena.issue.detectors.compilation_unit.MachineLearningDetector;
import cz.muni.jena.issue.detectors.machine_learning.InferenceQueueHolder;
import cz.muni.jena.parser.MachineLearningDetectorCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InferenceFacadeTest {

    @Mock
    private InferenceService inferenceService;

    @Mock
    private InferenceQueueHolder<EvaluationModel> inferenceQueueHolder;

    @Mock
    private MachineLearningDetector machineLearningDetector;

    @Mock
    private IssueMetadataService issueMetadataService;

    @Mock
    private InferenceConfiguration inferenceConfiguration;

    @InjectMocks
    private InferenceFacade inferenceFacade;

    @Test
    void canUseMachineLearning_serverReady_returnsTrue() {
        when(inferenceService.isServerReady()).thenReturn(true);

        assertThat(inferenceFacade.canUseMachineLearning()).isTrue();
    }

    @Test
    void startQueues_serverUnavailable_doesNotStartQueues() {
        when(inferenceService.isServerReady()).thenReturn(false);

        inferenceFacade.startQueues();

        verify(inferenceQueueHolder, never()).startQueues();
    }

    @Test
    void startQueues_serverReady_startsQueues() {
        when(inferenceService.isServerReady()).thenReturn(true);

        inferenceFacade.startQueues();

        verify(inferenceQueueHolder).startQueues();
    }

    @Test
    void terminateQueuesAndWait_serverUnavailable_returnsEmptyStream() {
        when(inferenceService.isServerReady()).thenReturn(false);

        List<IssueWithLazyMeta> foundIssues = inferenceFacade.terminateQueuesAndWait().toList();

        assertThat(foundIssues).isEmpty();
        verify(inferenceQueueHolder, never()).terminateQueuesAndWait();
    }

    @Test
    void terminateQueuesAndWait_serverReady_delegatesToQueueHolder() {
        when(inferenceService.isServerReady()).thenReturn(true);
        IssueWithLazyMeta issue = new IssueWithLazyMeta(new Issue(IssueType.GOD_DI_CLASS, 1, "com.example.Test"), null);
        when(inferenceQueueHolder.terminateQueuesAndWait()).thenReturn(Stream.of(issue));

        List<IssueWithLazyMeta> foundIssues = inferenceFacade.terminateQueuesAndWait().toList();

        assertThat(foundIssues).containsExactly(issue);
    }

    @Test
    void startMachineLearningEvaluation_disabled_returnsEmptyAndDoesNothing() {
        Optional<MachineLearningDetectorCallback> callback = inferenceFacade.startMachineLearningEvaluation(
                false,
                Set.of(IssueCategory.DI),
                new Configuration(null, null, null, null, null)
        );

        assertThat(callback).isEmpty();
        verifyNoInteractions(inferenceService, inferenceQueueHolder);
    }

    @Test
    void startMachineLearningEvaluation_serverUnavailable_returnsEmpty() {
        when(inferenceService.isServerReady()).thenReturn(false);

        Optional<MachineLearningDetectorCallback> callback = inferenceFacade.startMachineLearningEvaluation(
                true,
                Set.of(IssueCategory.DI),
                new Configuration(null, null, null, null, null)
        );

        assertThat(callback).isEmpty();
        verify(inferenceQueueHolder, never()).startQueues();
    }

    @Test
    void startMachineLearningEvaluation_enabledAndServerReady_returnsCallbackAndStartsQueues() {
        when(inferenceService.isServerReady()).thenReturn(true);
        Configuration configuration = new Configuration(null, null, null, null, null);

        Optional<MachineLearningDetectorCallback> callback = inferenceFacade.startMachineLearningEvaluation(
                true,
                Set.of(IssueCategory.DI, IssueCategory.SECURITY),
                configuration
        );

        assertThat(callback).isPresent();
        assertThat(callback.get().machineLearningDetector()).isEqualTo(machineLearningDetector);
        assertThat(callback.get().configuration()).isEqualTo(configuration);
        assertThat(callback.get().inferenceConfiguration()).isEqualTo(inferenceConfiguration);
        verify(inferenceQueueHolder).startQueues();
    }

    @Test
    void endMachineLearningEvaluation_machineLearningDisabled_returnsEmpty() {
        List<Issue> issues = inferenceFacade.endMachineLearningEvaluation(false, "project");

        assertThat(issues).isEmpty();
        verifyNoInteractions(inferenceQueueHolder, issueMetadataService);
    }

    @Test
    void endMachineLearningEvaluation_serverUnavailable_returnsEmpty() {
        when(inferenceService.isServerReady()).thenReturn(false);

        List<Issue> issues = inferenceFacade.endMachineLearningEvaluation(true, "project");

        assertThat(issues).isEmpty();
        verify(inferenceQueueHolder, never()).terminateQueuesAndWait();
        verifyNoInteractions(issueMetadataService);
    }

    @Test
    void endMachineLearningEvaluation_serverReady_setsMetadataAndReturnsIssues() {
        when(inferenceService.isServerReady()).thenReturn(true);

        Issue issue1 = new Issue(IssueType.GOD_DI_CLASS, 1, "com.example.Class1");
        Issue issue2 = new Issue(IssueType.MULTI_SERVICE, 2, "com.example.Class2");
        IssueWithLazyMeta wrappedIssue1 = new IssueWithLazyMeta(issue1, null);
        IssueWithLazyMeta wrappedIssue2 = new IssueWithLazyMeta(issue2, null);
        when(inferenceQueueHolder.terminateQueuesAndWait()).thenReturn(Stream.of(wrappedIssue1, wrappedIssue2));

        List<Issue> issues = inferenceFacade.endMachineLearningEvaluation(true, "project-1");

        assertThat(issues).containsExactly(issue1, issue2);
        verify(issueMetadataService).setMetaDataToIssues(List.of(wrappedIssue1, wrappedIssue2), "project-1");
    }
}
