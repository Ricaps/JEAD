package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.LongProducerMethodDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class LongProducerMethodDetectorTest extends IssueDetectorTester
{
    @Test
    void longProducerMethodDetectorTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.LONG_PRODUCER_METHOD,
                                15,
                                "com.example.antipatterns.complex_producer_method.GreetingServiceFactory"
                        )
                },
                new LongProducerMethodDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
