package cz.muni.jena.issue.detectors.compilation_unit.mocking;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import org.junit.jupiter.api.Test;

import static cz.muni.jena.utils.TestFixtures.POWER_MOCK_USAGE_PROJECT;

class InappropriateMockingDetectorTest extends IssueDetectorTester
{
    @Test
    void inappropriateMockingDetectorTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.INAPPROPRIATE_METHOD_MOCKING,
                                18,
                                "cz.muni.antipatterns.MockingAntipatternsTest"
                        ),
                        new Issue(
                                IssueType.INAPPROPRIATE_METHOD_MOCKING,
                                29,
                                "cz.muni.antipatterns.MockingAntipatternsTest"
                        ),
                        new Issue(
                                IssueType.INAPPROPRIATE_METHOD_MOCKING,
                                37,
                                "cz.muni.antipatterns.MockingAntipatternsTest"
                        )
                },
                new InappropriateMethodMockingDetector(), Configuration.readConfiguration()
        );
    }

    @Test
    void usageOfPowerMockTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.INAPPROPRIATE_METHOD_MOCKING,
                                18,
                                "MockingUsingPowerMockTest"
                        )
                },
                new InappropriateMethodMockingDetector(),
                Configuration.readConfiguration(),
                POWER_MOCK_USAGE_PROJECT
        );
    }
}
