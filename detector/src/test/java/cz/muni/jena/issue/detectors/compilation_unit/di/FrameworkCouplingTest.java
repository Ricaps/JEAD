package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.FrameworkCouplingDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class FrameworkCouplingTest extends IssueDetectorTester
{
    @Test
    void frameworkCouplingTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.FRAMEWORK_COUPLING,
                                11,
                                "com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection.MultipleFormsOfInjectionGreetingController"
                        ),
                        new Issue(
                                IssueType.FRAMEWORK_COUPLING,
                                14,
                                "com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection.MultipleFormsOfInjectionGreetingController"
                        ),
                        new Issue(
                                IssueType.FRAMEWORK_COUPLING,
                                25,
                                "com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection.MultipleFormsOfInjectionGreetingController"
                        )
                },
                new FrameworkCouplingDetector(), TestConfigLoader.readConfiguration()
        );
    }
}
