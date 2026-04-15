package cz.muni.jena.frontend.commands.export;

import cz.muni.jena.issue.AnalysisType;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueClass;
import cz.muni.jena.issue.IssueMethod;
import cz.muni.jena.issue.IssueType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportHeaderAlignmentTest {

    @TempDir
    Path tempDir;

    @Test
    void issuesExporter_headerMatchesWrittenValues() throws Exception {
        Issue issue = sampleIssue();

        IssuesCSVExporter exporter = new IssuesCSVExporter(new GenericCSVExporter());
        exporter.exportDataToCSVFile(tempDir.toString(), List.of(issue));

        Path csvPath = tempDir.resolve("issues.csv");
        try (CSVParser parser = parse(csvPath)) {
            List<CSVRecord> records = parser.getRecords();

            assertThat(parser.getHeaderMap().keySet())
                    .containsExactly(
                            "id",
                            "issue_type",
                            "analysis_type",
                            "line_number",
                            "fully_qualified_name",
                            "project_label",
                            "method_id",
                            "class_id"
                    );
            assertThat(records).hasSize(1);

            CSVRecord record = records.getFirst();
            assertThat(record.get("id")).isEqualTo("11");
            assertThat(record.get("issue_type")).isEqualTo(issue.getIssueType().toString());
            assertThat(record.get("analysis_type")).isEqualTo(issue.getAnalysisType().name());
            assertThat(record.get("line_number")).isEqualTo(issue.getLineNumber());
            assertThat(record.get("fully_qualified_name")).isEqualTo(issue.getFullyQualifiedName());
            assertThat(record.get("project_label")).isEqualTo(issue.getProjectLabel());
            assertThat(record.get("method_id")).isEqualTo("33");
            assertThat(record.get("class_id")).isEqualTo("22");
        }
    }

    @Test
    void joinedDataExporter_headerMatchesWrittenValues() throws Exception {
        Issue issue = sampleIssue();

        JoinedDataCSVExporter exporter = new JoinedDataCSVExporter(new GenericCSVExporter());
        exporter.exportDataToCSVFile(tempDir.toString(), List.of(issue));

        Path csvPath = tempDir.resolve("JoinedData.csv");
        try (CSVParser parser = parse(csvPath)) {
            List<CSVRecord> records = parser.getRecords();

            assertThat(parser.getHeaderMap().keySet())
                    .containsExactly(
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
                    );
            assertThat(records).hasSize(1);

            CSVRecord record = records.getFirst();
            assertThat(record.get("id")).isEqualTo("11");
            assertThat(record.get("issue_type")).isEqualTo(issue.getIssueType().toString());
            assertThat(record.get("analysis_type")).isEqualTo(issue.getAnalysisType().name());
            assertThat(record.get("line_number")).isEqualTo(issue.getLineNumber());
            assertThat(record.get("fully_qualified_name")).isEqualTo(issue.getFullyQualifiedName());
            assertThat(record.get("project_label")).isEqualTo(issue.getProjectLabel());
            assertThat(record.get("class_id")).isEqualTo("22");
            assertThat(record.get("class_complexity")).isEqualTo("7");
            assertThat(record.get("class_name")).isEqualTo("SampleClass");
            assertThat(record.get("method_id")).isEqualTo("33");
            assertThat(record.get("method_complexity")).isEqualTo("5");
            assertThat(record.get("method_name")).isEqualTo("sampleMethod");
        }
    }

    private CSVParser parse(Path csvPath) throws Exception {
        return CSVParser.parse(
                Files.readString(csvPath),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
        );
    }

    private Issue sampleIssue() {
        IssueClass issueClass = new IssueClass();
        issueClass.setId(22L);
        issueClass.setComplexity(7L);
        issueClass.setName("SampleClass");

        IssueMethod issueMethod = new IssueMethod();
        issueMethod.setId(33L);
        issueMethod.setComplexity(5L);
        issueMethod.setName("sampleMethod");

        Issue issue = new Issue(IssueType.UNUSED_INJECTION, 123, "com.example.SampleClass");
        issue.setId(11L);
        issue.setProjectLabel("sample-project");
        issue.setAnalysisType(AnalysisType.MACHINE_LEARNING);
        issue.setIssueClass(issueClass);
        issue.setMethod(issueMethod);
        return issue;
    }
}

