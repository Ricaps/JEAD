package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.GodDIClassDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class GodDIClassDetectorTest extends IssueDetectorTester
{
    @Test
    void godDIClassDetectorTestTwoConstructors()
    {
        Configuration configuration = TestConfigLoader.readConfiguration();
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.GOD_DI_CLASS,
                                null,
                                "com.example.antipatterns.open_window_injection.OpenWindowInjectionGreetingController"
                        )
                },
                new GodDIClassDetector(), new DIConfiguration(
                        configuration.diConfiguration().injectionAnnotations(),
                        configuration.diConfiguration().beanAnnotations(),
                        1,
                        configuration.diConfiguration().maxProducerMethodComplexity(),
                        configuration.diConfiguration().producerAnnotations(),
                        configuration.diConfiguration().directContainerCallMethods()
                )
        );
    }

    @Test
    void godDIClassDetectorTestNoInjectConstructor()
    {
        Configuration configuration = TestConfigLoader.readConfiguration();
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.GOD_DI_CLASS,
                                null,
                                "com.example.antipatterns.open_window_injection.GodDIController"
                        )
                },
                new GodDIClassDetector(), new DIConfiguration(
                        configuration.diConfiguration().injectionAnnotations(),
                        configuration.diConfiguration().beanAnnotations(),
                        1,
                        configuration.diConfiguration().maxProducerMethodComplexity(),
                        configuration.diConfiguration().producerAnnotations(),
                        configuration.diConfiguration().directContainerCallMethods()
                )
        );
    }
}
