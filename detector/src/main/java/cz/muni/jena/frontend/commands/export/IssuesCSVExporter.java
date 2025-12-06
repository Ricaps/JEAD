package cz.muni.jena.frontend.commands.export;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueClass;
import cz.muni.jena.issue.IssueMethod;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class IssuesCSVExporter implements CSVExporter
{
    private static final String[] ISSUES_HEADER = {
            "id",
            "issue_type",
            "line_number",
            "fully_qualified_name",
            "project_label",
            "method_id",
            "class_id"
    };
    private final GenericCSVExporter genericCSVExporter;

    public IssuesCSVExporter(GenericCSVExporter genericCSVExporter)
    {
        this.genericCSVExporter = genericCSVExporter;
    }

    @Override
    public String exportDataToCSVFile(String exportDirectory, Collection<Issue> issues)
    {
        String fileName = exportDirectory + "/issues.csv";
        genericCSVExporter.exportData(
                fileName,
                ISSUES_HEADER,
                issues,
                (issue, printer) -> {
                    try {
                        printer.printRecord(
                                issue.getId(),
                                issue.getIssueType(),
                                issue.getLineNumber(),
                                issue.getFullyQualifiedName(),
                                issue.getProjectLabel(),
                                Optional.ofNullable(issue.getMethod()).map(IssueMethod::getId).orElse(null),
                                Optional.ofNullable(issue.getIssueClass()).map(IssueClass::getId).orElse(null),
                                issue.getAnalysisType().name()
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        return "The anti-patterns have been exported to: " + fileName;
    }
}
