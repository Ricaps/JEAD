package cz.muni.jena.issue.detectors.compilation_unit.security;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

import static cz.muni.jena.utils.TestFixtures.AUTHORIZATION_SERVER_PROJECT;

class SigningJWTWithFixedSecretDetectorTest extends IssueDetectorTester
{

    @Test
    void signingJWTWithFixedSecretDetectorTest()
    {
        Configuration configuration = TestConfigLoader.readConfiguration();
        IssueDetector issueDetector = new SigningJWTWithFixedSecretDetector();
        verifyDetectorFindsIssues(
                new Issue[] {
                        new Issue(
                                IssueType.SIGNING_JWT_WITH_FIXED_SECRET,
                                195,
                                "example.OAuth2AuthorizationServerSecurityConfiguration"
                        ),
                        new Issue(
                                IssueType.SIGNING_JWT_WITH_FIXED_SECRET,
                                205,
                                "example.OAuth2AuthorizationServerSecurityConfiguration"
                        )
                },
                issueDetector,
                configuration,
                AUTHORIZATION_SERVER_PROJECT
        );
    }
}
