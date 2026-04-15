package cz.muni.jena.frontend.commands.export;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueClass;
import cz.muni.jena.issue.IssueMethod;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class JoinedDataCSVExporter implements CSVExporter
{
    private static final String[] ISSUES_HEADER = {
            "id",
            "issue_type",
            "analysis_type",
            "line_number",
            "fully_qualified_name",
            "project_label",
            "class_id",
            "class_complexity",
            "class_name",
            "method_id",
            "method_complexity",
            "method_name"
    };
    private final GenericCSVExporter genericCSVExporter;

    public JoinedDataCSVExporter(GenericCSVExporter genericCSVExporter)
    {
        this.genericCSVExporter = genericCSVExporter;
    }

    @Override
    public String exportDataToCSVFile(String exportDirectory, Collection<Issue> issues)
    {
        String fileName = exportDirectory + "/JoinedData.csv";
        genericCSVExporter.exportData(
                fileName,
                ISSUES_HEADER,
                issues,
                (issue, printer) -> {
                    try {
                        printer.printRecord(
                                issue.getId(),
                                issue.getIssueType(),
                                issue.getAnalysisType(),
                                issue.getLineNumber(),
                                issue.getFullyQualifiedName(),
                                issue.getProjectLabel(),
                                Optional.ofNullable(issue.getIssueClass()).map(IssueClass::getId).orElse(null),
                                Optional.ofNullable(issue.getIssueClass()).map(IssueClass::getComplexity).orElse(null),
                                Optional.ofNullable(issue.getIssueClass()).map(IssueClass::getName).orElse(null),
                                Optional.ofNullable(issue.getMethod()).map(IssueMethod::getId).orElse(null),
                                Optional.ofNullable(issue.getMethod()).map(IssueMethod::getComplexity).orElse(null),
                                Optional.ofNullable(issue.getMethod()).map(IssueMethod::getName).orElse(null)
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        return "The joined data has been exported to: " + fileName;
    }
}
