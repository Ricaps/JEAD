package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.codeminer.extractor.comments.CommentType;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.codeminer.extractor.comments.model.CommentDto;
import cz.muni.jena.codeminer.extractor.comments.model.CommentsMapper;
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

    private List<Comment> sampleComments() {
        return List.of(
                new Comment(CommentType.JAVADOC, "alpha-doc", 10, "com.example.Doc"),
                new Comment(CommentType.LINE, "beta-line", 20, "com.example.Line"),
                new Comment(CommentType.BLOCK, "gamma-block", 30, "com.example.Block")
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
    }
}


