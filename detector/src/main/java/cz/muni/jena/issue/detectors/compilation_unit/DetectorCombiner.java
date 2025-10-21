package cz.muni.jena.issue.detectors.compilation_unit;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.stream.Stream;

public class DetectorCombiner implements IssueDetector
{
    private final List<IssueDetector> issueDetectors;

    public DetectorCombiner(List<IssueDetector> issueDetectors)
    {
        this.issueDetectors = issueDetectors;
    }

    @Override
    @NonNull
    public Stream<Issue> findIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration)
    {
        return issueDetectors
                .stream()
                .flatMap(issueDetector -> issueDetector.findIssues(classOrInterfaceDeclaration,
                                                                   configuration));
    }
}
