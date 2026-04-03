package cz.muni.jena.issue.detectors.compilation_unit.security;

import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.IssueDetectorTester;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.issue.detectors.compilation_unit.security.token_lifetime.LifelongValidAccessTokensDetector;
import cz.muni.jena.utils.TestConfigLoader;
import org.junit.jupiter.api.Test;

import static cz.muni.jena.utils.TestFixtures.AUTHORIZATION_SERVER_PROJECT;

class LifelongValidAccessTokensDetectorTest extends IssueDetectorTester
{

    @Test
    void lifelongValidAccessTokensDetectorTest()
    {
        Configuration configuration = TestConfigLoader.readConfiguration();
        IssueDetector issueDetector = new LifelongValidAccessTokensDetector();
        verifyDetectorFindsIssues(
                new Issue[] {
                        new Issue(
                                IssueType.LIFELONG_ACCESS_TOKENS,
                                116,
                                "example.OAuth2AuthorizationServerSecurityConfiguration"
                        )
                },
                issueDetector,
                configuration,
                AUTHORIZATION_SERVER_PROJECT
        );
    }
}
