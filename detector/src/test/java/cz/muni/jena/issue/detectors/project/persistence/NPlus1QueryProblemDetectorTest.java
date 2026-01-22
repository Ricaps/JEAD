package cz.muni.jena.issue.detectors.project.persistence;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.project.NPlus1QueryProblemDetector;
import cz.muni.jena.issue.detectors.project.ProjectIssueDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cz.muni.jena.Preconditions.verifyCorrectWorkingDirectory;
import static cz.muni.jena.utils.TestFixtures.ANTIPATTERNS_PROJECT;
import static org.assertj.core.api.Assertions.assertThat;

class NPlus1QueryProblemDetectorTest extends IssueDetectorTester
{
    @Test
    void nPlus1QueryProblemDetectorTest()
    {
        verifyCorrectWorkingDirectory();
        ProjectIssueDetector issueDetector = new NPlus1QueryProblemDetector();
        List<Issue> issues = issueDetector.findIssues(
                ANTIPATTERNS_PROJECT,
                Configuration.readConfiguration()
        ).toList();
        assertThat(issues).containsExactlyInAnyOrder(
                new Issue(
                        IssueType.N_PLUS1_QUERY_PROBLEM,
                        19,
                        "com.example.antipatterns.query_problems.comment.CommentDaoWithEM"
                ),
                new Issue(
                        IssueType.N_PLUS1_QUERY_PROBLEM,
                        31,
                        "com.example.antipatterns.query_problems.comment.CommentDaoWithEM"
                ),
                new Issue(
                        IssueType.N_PLUS1_QUERY_PROBLEM,
                        44,
                        "com.example.antipatterns.query_problems.comment.CommentDaoWithEM"
                ),
                new Issue(
                        IssueType.N_PLUS1_QUERY_PROBLEM,
                        12,
                        "com.example.antipatterns.query_problems.comment.CommentDao"
                ),
                new Issue(
                        IssueType.N_PLUS1_QUERY_PROBLEM,
                        18,
                        "com.example.antipatterns.query_problems.comment.CommentDao"
                )
        );
    }
}
