package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.codeminer.extractor.comments.CommentType;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.codeminer.extractor.comments.model.CommentDto;
import cz.muni.jena.codeminer.extractor.comments.model.CommentsMapper;
import cz.muni.jena.codeminer.extractor.god_di.EvaluationModelProvider;
import cz.muni.jena.codeminer.extractor.god_di.model.DIMetrics;
import cz.muni.jena.codeminer.extractor.god_di.model.DIMetricsDto;
import cz.muni.jena.codeminer.extractor.god_di.model.DIMetricsMapper;
import cz.muni.jena.codeminer.extractor.multi_service.model.MultiServiceMethods;
import cz.muni.jena.codeminer.extractor.multi_service.model.MultiServiceMethodsDto;
import cz.muni.jena.codeminer.extractor.multi_service.model.MultiServiceMethodsMapper;
import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistryConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        OutputConfig.class,
        OutputFormatterFactory.class,
        ModelMapperRegistryConfig.class,
        OutputFormatterWorkflowIT.WorkflowConfig.class
})
class OutputFormatterWorkflowIT {

    @Inject
    private OutputFormatterFactory outputFormatterFactory;

    @Named("primaryObjectMapper")
    @Inject
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @Test
    void factoryWorkflow_json_usesProductionCommentMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow.json");
        List<Comment> comments = sampleComments();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("json").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(comments);
        }

        List<Map<String, Object>> rows = objectMapper.readValue(Files.readString(outputFile), new TypeReference<>() {
        });
        assertCommentDtoRows(rows, comments);
    }

    @Test
    void factoryWorkflow_jsonl_usesProductionCommentMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow.jsonl");
        List<Comment> comments = sampleComments();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("jsonl").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(comments);
        }

        List<Map<String, Object>> rows = Files.readAllLines(outputFile).stream()
                .filter(line -> !line.isBlank())
                .map(this::parseJsonRow)
                .toList();

        assertThat(rows).hasSize(comments.size());
        assertCommentDtoRows(rows, comments);
    }

    @Test
    void factoryWorkflow_csv_usesProductionCommentMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow.csv");
        List<Comment> comments = sampleComments();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("csv").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(comments);
        }

        try (CSVParser parser = CSVParser.parse(
                Files.readString(outputFile),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
        )) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(parser.getHeaderMap().keySet()).containsExactly("text", "commentType");
            assertThat(records).hasSize(comments.size());

            for (int i = 0; i < comments.size(); i++) {
                Comment comment = comments.get(i);
                CSVRecord record = records.get(i);
                assertThat(record.get("text")).isEqualTo(comment.text());
                assertThat(record.get("commentType")).isEqualTo(comment.commentType().name());
            }
        }
    }

    @Test
    void factoryWorkflow_json_usesProductionDIMetricsMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-di.json");
        List<DIMetrics> metrics = sampleDIMetrics();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("json").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(metrics);
        }

        List<Map<String, Object>> rows = objectMapper.readValue(Files.readString(outputFile), new TypeReference<>() {
        });
        assertDIMetricsDtoRows(rows, metrics);
    }

    @Test
    void factoryWorkflow_jsonl_usesProductionDIMetricsMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-di.jsonl");
        List<DIMetrics> metrics = sampleDIMetrics();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("jsonl").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(metrics);
        }

        List<Map<String, Object>> rows = Files.readAllLines(outputFile).stream()
                .filter(line -> !line.isBlank())
                .map(this::parseJsonRow)
                .toList();

        assertThat(rows).hasSize(metrics.size());
        assertDIMetricsDtoRows(rows, metrics);
    }

    @Test
    void factoryWorkflow_csv_usesProductionDIMetricsMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-di.csv");
        List<DIMetrics> metrics = sampleDIMetrics();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("csv").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(metrics);
        }

        try (CSVParser parser = CSVParser.parse(
                Files.readString(outputFile),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
        )) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(parser.getHeaderMap().keySet()).containsExactly("loc", "cc", "nooa", "LCOM5", "noom", "nocm");
            assertThat(records).hasSize(metrics.size());

            for (int i = 0; i < metrics.size(); i++) {
                DIMetrics metric = metrics.get(i);
                CSVRecord record = records.get(i);
                assertThat(record.get("loc")).isEqualTo(String.valueOf(metric.linesOfCode()));
                assertThat(record.get("cc")).isEqualTo(String.valueOf(metric.cyclomaticComplexity()));
                assertThat(record.get("nooa")).isEqualTo(String.valueOf(metric.injectedFields()));
                assertThat(record.get("LCOM5")).isEqualTo(String.valueOf(metric.lcom5()));
                assertThat(record.get("noom")).isEqualTo(String.valueOf(metric.methodsCount()));
                assertThat(record.get("nocm")).isEqualTo(String.valueOf(metric.staticMethodsCount()));
            }
        }
    }

    @Test
    void factoryWorkflow_json_usesProductionMultiServiceMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-ms.json");
        List<MultiServiceMethods> methods = sampleMultiServiceMethods();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("json").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(methods);
        }

        List<Map<String, Object>> rows = objectMapper.readValue(Files.readString(outputFile), new TypeReference<>() {
        });
        assertMultiServiceDtoRows(rows, methods);
    }

    @Test
    void factoryWorkflow_jsonl_usesProductionMultiServiceMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-ms.jsonl");
        List<MultiServiceMethods> methods = sampleMultiServiceMethods();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("jsonl").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(methods);
        }

        List<Map<String, Object>> rows = Files.readAllLines(outputFile).stream()
                .filter(line -> !line.isBlank())
                .map(this::parseJsonRow)
                .toList();

        assertThat(rows).hasSize(methods.size());
        assertMultiServiceDtoRows(rows, methods);
    }

    @Test
    void factoryWorkflow_csv_usesProductionMultiServiceMapper() throws Exception {
        Path outputFile = tempDir.resolve("workflow-ms.csv");
        List<MultiServiceMethods> methods = sampleMultiServiceMethods();

        try (OutputFormatter formatter = outputFormatterFactory.getCodeSerializer("csv").orElseThrow()) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(methods);

            // Current CsvOutput cannot serialize nested object values in a single cell.
            assertThatThrownBy(formatter::close)
                    .isInstanceOf(tools.jackson.dataformat.csv.CsvWriteException.class);
        }
    }

    private List<Comment> sampleComments() {
        return List.of(
                new Comment(CommentType.JAVADOC, "alpha-doc", 10, "com.example.Doc"),
                new Comment(CommentType.LINE, "beta-line", 20, "com.example.Line"),
                new Comment(CommentType.BLOCK, "gamma-block", 30, "com.example.Block")
        );
    }

    private List<DIMetrics> sampleDIMetrics() {
        return List.of(
                new DIMetrics(120, 16L, 5L, 0.42, 10, 2, "code-1", new EvaluationModelProvider("com.example.DI1", 11)),
                new DIMetrics(80, 9L, 3L, 0.33, 7, 1, "code-2", new EvaluationModelProvider("com.example.DI2", 22))
        );
    }

    private List<MultiServiceMethods> sampleMultiServiceMethods() {
        return List.of(
                new MultiServiceMethods(
                        List.of(
                                new MultiServiceMethods.Method("syncUsers", "void syncUsers()"),
                                new MultiServiceMethods.Method("sendInvoice", "void sendInvoice(String id)")
                        ),
                        2,
                        "com.example.Multi1",
                        15
                ),
                new MultiServiceMethods(
                        List.of(new MultiServiceMethods.Method("reindex", "void reindex(int batchSize)")),
                        1,
                        "com.example.Multi2",
                        33
                )
        );
    }

    private Map<String, Object> parseJsonRow(String line) {
        return objectMapper.readValue(line, new TypeReference<>() {
        });
    }

    private void assertCommentDtoRows(List<Map<String, Object>> rows, List<Comment> comments) {
        assertThat(rows).hasSize(comments.size());

        for (int i = 0; i < comments.size(); i++) {
            Comment expected = comments.get(i);
            Map<String, Object> row = rows.get(i);

            assertThat(row).containsOnlyKeys("text", "commentType");
            assertThat(row.get("text")).isEqualTo(expected.text());
            assertThat(row.get("commentType")).isEqualTo(expected.commentType().name());
        }
    }

    private void assertDIMetricsDtoRows(List<Map<String, Object>> rows, List<DIMetrics> metrics) {
        assertThat(rows).hasSize(metrics.size());

        for (int i = 0; i < metrics.size(); i++) {
            DIMetrics expected = metrics.get(i);
            Map<String, Object> row = rows.get(i);

            assertThat(row).containsOnlyKeys("loc", "cc", "nooa", "LCOM5", "noom", "nocm");
            assertThat(((Number) row.get("loc")).intValue()).isEqualTo(expected.linesOfCode());
            assertThat(((Number) row.get("cc")).longValue()).isEqualTo(expected.cyclomaticComplexity());
            assertThat(((Number) row.get("nooa")).longValue()).isEqualTo(expected.injectedFields());
            assertThat(((Number) row.get("LCOM5")).doubleValue()).isEqualTo(expected.lcom5());
            assertThat(((Number) row.get("noom")).intValue()).isEqualTo(expected.methodsCount());
            assertThat(((Number) row.get("nocm")).intValue()).isEqualTo(expected.staticMethodsCount());
        }
    }

    @SuppressWarnings("unchecked")
    private void assertMultiServiceDtoRows(List<Map<String, Object>> rows, List<MultiServiceMethods> methods) {
        assertThat(rows).hasSize(methods.size());

        for (int i = 0; i < methods.size(); i++) {
            MultiServiceMethods expected = methods.get(i);
            Map<String, Object> row = rows.get(i);
            List<Map<String, Object>> methodRows = (List<Map<String, Object>>) row.get("methods");

            assertThat(row).containsOnlyKeys("methods");
            assertThat(methodRows).hasSize(expected.methods().size());

            for (int j = 0; j < expected.methods().size(); j++) {
                MultiServiceMethods.Method expectedMethod = expected.methods().get(j);
                Map<String, Object> methodRow = methodRows.get(j);

                assertThat(methodRow.get("name")).isEqualTo(expectedMethod.name());
                assertThat(methodRow.get("signature")).isEqualTo(expectedMethod.signature());
            }
        }
    }

    @TestConfiguration
    static class WorkflowConfig {

        @Bean
        @Primary
        ObjectMapper primaryObjectMapper(@Qualifier("objectMapper") ObjectMapper objectMapper) {
            return objectMapper;
        }

        @Bean
        ModelDtoMapper<Comment, CommentDto> commentsMapper() {
            return Mappers.getMapper(CommentsMapper.class);
        }

        @Bean
        ModelDtoMapper<DIMetrics, DIMetricsDto> diMetricsMapper() {
            return Mappers.getMapper(DIMetricsMapper.class);
        }

        @Bean
        ModelDtoMapper<MultiServiceMethods, MultiServiceMethodsDto> multiServiceMethodsMapper() {
            return Mappers.getMapper(MultiServiceMethodsMapper.class);
        }
    }
}


