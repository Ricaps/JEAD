package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.OpenDoorInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class OpenDoorInjectionDetectorTest extends IssueDetectorTester
{
    @Test
    void openDoorInjectionTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.OPEN_DOOR_INJECTION,
                                27,
                                "com.example.antipatterns.open_door_injection.OpenDoorInjectionUsingConstructorGreetingController"
                        ),
                        new Issue(
                                IssueType.OPEN_DOOR_INJECTION,
                                22,
                                "com.example.antipatterns.open_door_injection.OpenDoorInjectionUsingSetterGreetingController"
                        )
                },
                new OpenDoorInjectionDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
