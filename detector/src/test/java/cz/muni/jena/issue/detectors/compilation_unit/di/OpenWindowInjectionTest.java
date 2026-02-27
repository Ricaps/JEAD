package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.OpenWindowInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class OpenWindowInjectionTest extends IssueDetectorTester
{
    @Test
    void findOpenWindowInjectionsTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.OPEN_WINDOW_INJECTION,
                                24,
                                "com.example.antipatterns.open_window_injection.OpenWindowInjectionGreetingController"
                        ),
                        new Issue(
                                IssueType.OPEN_WINDOW_INJECTION,
                                29,
                                "com.example.antipatterns.open_window_injection.OpenWindowInjectionGreetingController"
                        )
                },
                new OpenWindowInjectionDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
