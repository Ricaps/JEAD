package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.MultipleFormsOfInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class MultipleFormsOfInjectionTest extends IssueDetectorTester
{
    @Test
    void multipleFormsOfInjectionTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.MULTIPLE_FORMS_OF_INJECTION,
                                11,
                                "com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection.MultipleFormsOfInjectionGreetingController"
                        )
                },
                new MultipleFormsOfInjectionDetector(),
                TestConfigLoader.readConfiguration()
        );
    }
}
