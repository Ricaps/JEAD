package cz.muni.jena.issue.detectors.compilation_unit.di;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.ConcreteClassInjectionDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

class ConcreteClassInjectionDetectorTest extends IssueDetectorTester
{
    @Test
    void findConcreteClassInjectionsTest()
    {
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.CONCRETE_CLASS_INJECTION,
                                20,
                                "com.example.antipatterns.concrete_class_injection.GreetingControllerWithSetter"
                        ),
                        new Issue(
                                IssueType.CONCRETE_CLASS_INJECTION,
                                14,
                                "com.example.antipatterns.concrete_class_injection.GreetingControllerUsingConstructor"
                        ),
                        new Issue(
                                IssueType.CONCRETE_CLASS_INJECTION,
                                12,
                                "com.example.antipatterns.concrete_class_injection.GreetingsControllerWithFieldInjection"
                        )},
                new ConcreteClassInjectionDetector(), TestConfigLoader.readConfiguration()
        );
    }
}
