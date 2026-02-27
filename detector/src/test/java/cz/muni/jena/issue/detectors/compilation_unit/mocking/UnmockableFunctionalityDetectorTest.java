package cz.muni.jena.issue.detectors.compilation_unit.mocking;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class UnmockableFunctionalityDetectorTest extends IssueDetectorTester
{
    @Test
    void unmockableFunctionalityDetectorTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.CONSTRUCTOR_CALL_WITH_EXCEPTION,
                                46,
                                "com.example.antipatterns.AntiPatterns"
                        ),
                        new Issue(
                                IssueType.FINAL_METHOD_CALL_WITH_EXCEPTION,
                                47,
                                "com.example.antipatterns.AntiPatterns"
                        ),
                        new Issue(
                                IssueType.STATIC_METHOD_CALL_WITH_EXCEPTION,
                                48,
                                "com.example.antipatterns.AntiPatterns"
                        ),
                        new Issue(
                                IssueType.FINAL_METHOD_CALL_WITH_EXCEPTION,
                                49,
                                "com.example.antipatterns.AntiPatterns"
                        )
                },
                new UnmockableFunctionalityDetector(), TestConfigLoader.readConfiguration()
        );
    }
}
