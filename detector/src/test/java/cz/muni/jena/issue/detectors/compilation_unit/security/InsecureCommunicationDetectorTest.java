package cz.muni.jena.issue.detectors.compilation_unit.security;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

import static cz.muni.jena.Preconditions.verifyCorrectWorkingDirectory;
import static cz.muni.jena.utils.TestFixtures.AUTHORIZATION_SERVER_PROJECT;

class InsecureCommunicationDetectorTest extends IssueDetectorTester
{
    @Test
    void insecureCommunicationDetectorTest()
    {
        verifyCorrectWorkingDirectory();
        Configuration configuration = TestConfigLoader.readConfiguration();
        IssueDetector issueDetector = new InsecureCommunicationDetector();
        verifyDetectorFindsIssues(
                new Issue[]{
                        new Issue(
                                IssueType.INSECURE_COMMUNICATION,
                                102,
                                "example.OAuth2AuthorizationServerSecurityConfiguration"
                        ),
                        new Issue(
                                IssueType.INSECURE_COMMUNICATION,
                                103,
                                "example.OAuth2AuthorizationServerSecurityConfiguration"
                        )
                },
                issueDetector,
                configuration,
                AUTHORIZATION_SERVER_PROJECT
        );
    }
}
