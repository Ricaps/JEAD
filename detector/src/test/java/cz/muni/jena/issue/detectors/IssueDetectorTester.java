package cz.muni.jena.issue.detectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueMetadataService;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.issue.detectors.compilation_unit.dependency.DIIssueDetector;
import cz.muni.jena.parser.AsyncCompilationUnitParser;
import cz.muni.jena.parser.IssueDetectorCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static cz.muni.jena.Preconditions.verifyCorrectWorkingDirectory;
import static cz.muni.jena.utils.TestFixtures.ANTIPATTERNS_PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueDetectorTester
{

    public void verifyDetectorFindsIssues(Issue[] issuesExpected, DIIssueDetector detector, DIConfiguration configuration)
    {
        verifyDetectorFoundIssues(
                compilationUnits -> detector.findIssues(compilationUnits, configuration),
                issuesExpected, ANTIPATTERNS_PROJECT
        );
    }

    public void verifyDetectorFindsIssues(Issue[] issuesExpected, IssueDetector detector, Configuration configuration)
    {
        verifyDetectorFindsIssues(issuesExpected, detector, configuration, ANTIPATTERNS_PROJECT);
    }

    public void verifyDetectorFindsIssues(Issue[] issuesExpected, IssueDetector detector, Configuration configuration, String project)
    {
        verifyDetectorFoundIssues(
                compilationUnits -> detector.findIssues(compilationUnits, configuration),
                issuesExpected, project
        );
    }

    public void verifyDetectorFoundIssues(
            Function<ClassOrInterfaceDeclaration, Stream<Issue>> configuredDetector,
            Issue[] issuesExpected,
            String project
    )
    {
        verifyCorrectWorkingDirectory();
        IssueDetector issueDetector = (classOrInterfaceDeclaration, configuration) ->
                configuredDetector.apply(classOrInterfaceDeclaration);
        List<Issue> issues = Collections.synchronizedList(new ArrayList<>());
        SourceRoot.Callback callback = new IssueDetectorCallback(
                issueDetector,
                Configuration.readConfiguration(),
                issues,
                "projectLabel",
                mock(IssueMetadataService.class)
        );
        new AsyncCompilationUnitParser(project).processCompilationUnits(callback);
        assertThat(issues).contains(issuesExpected);
    }

}
