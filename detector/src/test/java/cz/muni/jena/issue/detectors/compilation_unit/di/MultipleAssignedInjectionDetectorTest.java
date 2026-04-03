package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.MultipleAssignedInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class MultipleAssignedInjectionDetectorTest extends IssueDetectorTester
{
    @Test
    void multipleAssignedInjectionDetectorTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.MULTIPLE_ASSIGNED_INJECTION,
                                17,
                                "com.example.antipatterns.multiple_assigned_injections.MultipleAssignedInjectionGreetingController"
                        ),
                        new Issue(
                                IssueType.MULTIPLE_ASSIGNED_INJECTION,
                                18,
                                "com.example.antipatterns.multiple_assigned_injections.MultipleAssignedInjectionGreetingController"
                        ),
                        new Issue(
                                IssueType.MULTIPLE_ASSIGNED_INJECTION,
                                19,
                                "com.example.antipatterns.multiple_assigned_injections.MultipleAssignedInjectionGreetingController"
                        )
                },
                new MultipleAssignedInjectionDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
