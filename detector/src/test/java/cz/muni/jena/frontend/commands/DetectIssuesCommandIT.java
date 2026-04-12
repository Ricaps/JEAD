package cz.muni.jena.frontend.commands;

import cz.muni.jena.inference.InferenceFacade;
import cz.muni.jena.issue.AnalysisType;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueCategory;
import cz.muni.jena.issue.IssueDao;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.testinfra.EnableDetectorContainers;
import cz.muni.jena.testinfra.SharedDetectorContainers;
import cz.muni.jena.utils.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableDetectorContainers
class DetectIssuesCommandIT {

    @Autowired
    private DetectIssuesCommand detectIssuesCommand;

    @Autowired
    private IssueDao issueDao;

    @Autowired
    private InferenceFacade inferenceFacade;

    @BeforeEach
    void cleanDatabase() {
        issueDao.deleteAll();
        waitForInferenceReady();
    }

    private void waitForInferenceReady() {
        SharedDetectorContainers.waitForInferenceReady(inferenceFacade, Duration.ofSeconds(90));
    }

    @Test
    void detectIssues_withMachineLearning_persistsTechnicalDebtIssues() {
        detectIssuesCommand.detectIssues(
                null,
                TestFixtures.ANTIPATTERNS_PROJECT,
                IssueCategory.TECHNICAL_DEBT,
                false,
                "tc-poc",
                true
        );

        List<Issue> issues = issueDao.findAll();

        assertThat(issues).isNotEmpty();
        assertThat(issues).anyMatch(issue -> issue.getIssueType() == IssueType.SELF_ADMITTED_TECHNICAL_DEBT);
        assertThat(issues).anyMatch(issue -> issue.getAnalysisType() == AnalysisType.MACHINE_LEARNING);
        assertThat(issues).allMatch(issue -> "tc-poc".equals(issue.getProjectLabel()));
    }
}
