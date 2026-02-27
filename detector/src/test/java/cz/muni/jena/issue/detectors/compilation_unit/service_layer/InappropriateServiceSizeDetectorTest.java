package cz.muni.jena.issue.detectors.compilation_unit.service_layer;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

import static cz.muni.jena.utils.TestFixtures.ANTIPATTERNS_PROJECT;

class InappropriateServiceSizeDetectorTest extends IssueDetectorTester
{
    @Test
    void inappropriateServiceSizeDetectorTest()
    {
        Configuration configuration = TestConfigLoader.readConfiguration();
        IssueDetector issueDetector = (
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
                Configuration config
        ) -> new InappropriateServiceSizeDetector().findIssues(
                classOrInterfaceDeclaration,
                2,
                3,
                configuration.serviceLayerConfiguration().serviceAnnotations()
        );

        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.TINY_SERVICE,
                                null,
                                "com.example.antipatterns.unused_injection.UnusedInjectionGreetingServiceImpl"
                        ),
                        new Issue(
                                IssueType.MULTI_SERVICE,
                                null,
                                "com.example.antipatterns.unused_injection.UnusedServiceImpl"
                        )
                },
                issueDetector,
                configuration,
                ANTIPATTERNS_PROJECT
        );
    }
}
