package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.DirectContainerCallDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;


class DirectContainerCallDetectorTest extends IssueDetectorTester
{
    @Test
    void findDirectContainerCallsTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.DIRECT_CONTAINER_CALL,
                                23,
                                "com.example.antipatterns.direct_container_call.DirectContainerCallGreetingController"
                        )
                },
                new DirectContainerCallDetector(), TestConfigLoader.readConfiguration()
        );
    }
}
