//package cz.muni.jena.frontend.commands;
//
//import cz.muni.jena.configuration.TestContainers;
//import cz.muni.jena.issue.Issue;
//import cz.muni.jena.issue.IssueDao;
//import cz.muni.jena.issue.IssueType;
//import cz.muni.jena.utils.NonShellIntegrationTest;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.containers.ComposeContainer;
//import org.testcontainers.junit.jupiter.Container;
//
//import java.nio.file.Path;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
////@NonShellIntegrationTest
//@Transactional
//public class DetectIssuesCommandIT {
//
//    @Container
//    @SuppressWarnings("unused")
//    private static final ComposeContainer composeContainer = TestContainers.getComposeContainer();
//    private static final Path ANTIPATTERNS_PROJECT_PATH = Path.of("../", "antipatterns").toAbsolutePath();
//
//    @Autowired
//    DetectIssuesCommand detectIssuesCommand;
//
//    @Autowired
//    IssueDao issueDao;
//
//    @Test
//    void enabledMachineLearning_foundIssues() {
//
//        detectIssuesCommand.detectIssues(
//                null,
//                ANTIPATTERNS_PROJECT_PATH.toString(),
//                null,
//                false,
//                "test-label",
//                true
//        );
//
//        List<Issue> technicalDebtIssues = issueDao.findAllByIssueType(IssueType.SELF_ADMITTED_TECHNICAL_DEBT);
//        assertThat(technicalDebtIssues).hasSize(4);
//        assertThat(technicalDebtIssues).allMatch(issue -> issue.getFullyQualifiedName().equals("com.example.antipatterns.ml_test.TestMachineLearningIntegration"));
//
//        List<Issue> commentedCodeIssues = issueDao.findAllByIssueType(IssueType.COMMENTED_OUT_CODE);
//        assertThat(commentedCodeIssues).hasSize(4);
//        assertThat(commentedCodeIssues).allMatch(issue -> issue.getFullyQualifiedName().equals("com.example.antipatterns.ml_test.TestMachineLearningIntegration"));
//    }
//
//    @Test
//    void disabledMachineLearning_noMLIssuesFound() {
//
//        detectIssuesCommand.detectIssues(
//                null,
//                ANTIPATTERNS_PROJECT_PATH.toString(),
//                null,
//                false,
//                "test-label",
//                false
//        );
//
//        List<Issue> technicalDebtIssues = issueDao.findAllByIssueType(IssueType.SELF_ADMITTED_TECHNICAL_DEBT);
//        assertThat(technicalDebtIssues).isEmpty();
//
//        List<Issue> commentedCodeIssues = issueDao.findAllByIssueType(IssueType.COMMENTED_OUT_CODE);
//        assertThat(commentedCodeIssues).isEmpty();
//    }
//
//}
//
