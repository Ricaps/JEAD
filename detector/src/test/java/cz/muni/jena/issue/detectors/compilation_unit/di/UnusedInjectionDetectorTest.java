package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.UnusedInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class UnusedInjectionDetectorTest extends IssueDetectorTester
{
    @Test
    void findUnusedInjections()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.UNUSED_INJECTION,
                                12,
                                "com.example.antipatterns.unused_injection.UnusedInjectionGreetingController"
                        )
                },
                new UnusedInjectionDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
