package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.dto.BaseDto;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.CsvMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class OutputFormatterFactoryTest {

    private static final int SAMPLE_SIZE = 45;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CsvMapper csvMapper = new CsvMapper();

    @TempDir
    Path tempDir;

    @Test
    void jsonOutput_writesExpectedJsonArray() throws Exception {
        Path outputFile = tempDir.resolve("out.json");
        List<TestEvaluationModel> expected = randomModels(100);

        try (OutputFormatter formatter = new JsonOutput(objectMapper, createRegistry())) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(expected.subList(0, 20));
            formatter.add(expected.subList(20, expected.size()));
        }

        List<Map<String, Object>> parsed = objectMapper.readValue(
                Files.readString(outputFile),
                new TypeReference<>() {
                }
        );

        assertThat(parsed).hasSize(expected.size());
        assertRowsMatch(parsed, expected);
    }

    @Test
    void jsonLinesOutput_writesExpectedRows() throws Exception {
        Path outputFile = tempDir.resolve("out.jsonl");
        List<TestEvaluationModel> expected = randomModels(200);

        try (OutputFormatter formatter = new JsonLinesOutput(objectMapper, createRegistry())) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(expected.subList(0, 31));
            formatter.add(expected.subList(31, expected.size()));
        }

        List<String> lines = Files.readAllLines(outputFile).stream()
                .filter(line -> !line.isBlank())
                .toList();

        assertThat(lines).hasSize(expected.size());

        List<Map<String, Object>> parsed = lines.stream()
                .map(this::parseJsonRow)
                .toList();

        assertRowsMatch(parsed, expected);
    }

    @Test
    void csvOutput_writesExpectedHeaderAndRows() throws Exception {
        Path outputFile = tempDir.resolve("out.csv");
        List<TestEvaluationModel> expected = randomModels(300);

        try (OutputFormatter formatter = new CsvOutput(csvMapper, objectMapper, createRegistry())) {
            formatter.setOutputPath(outputFile.toString());
            formatter.add(expected.subList(0, 32));
            formatter.add(expected.subList(32, expected.size()));
        }

        try (CSVParser parser = CSVParser.parse(
                Files.readString(outputFile),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
        )) {
            List<CSVRecord> records = parser.getRecords();

            assertThat(parser.getHeaderMap().keySet())
                    .containsExactly("fullyQualifiedName", "startLine", "label");
            assertThat(records).hasSize(expected.size());

            for (int i = 0; i < expected.size(); i++) {
                TestEvaluationModel model = expected.get(i);
                CSVRecord record = records.get(i);

                assertThat(record.get("fullyQualifiedName")).isEqualTo(model.fullyQualifiedName());
                assertThat(record.get("startLine")).isEqualTo(String.valueOf(model.startLine()));
                assertThat(record.get("label")).isEqualTo(model.label());
            }
        }
    }

    private ModelMapperRegistry createRegistry() {
        Map<Class<? extends EvaluationModel>, ModelDtoMapper<?, ?>> registry = new HashMap<>();
        registry.put(TestEvaluationModel.class, new TestModelMapper());
        return new ModelMapperRegistry(registry);
    }

    private List<TestEvaluationModel> randomModels(long seed) {
        Random random = new Random(seed);
        List<TestEvaluationModel> models = new ArrayList<>();

        for (int i = 0; i < OutputFormatterFactoryTest.SAMPLE_SIZE; i++) {
            String fqn = "com.example." + random.nextInt(1_000_000) + ".Class" + i;
            int startLine = random.nextInt(1, 10_000);
            String label = "label-" + random.nextInt(1000);
            models.add(new TestEvaluationModel(fqn, startLine, label));
        }

        return models;
    }

    private Map<String, Object> parseJsonRow(String line) {
        return objectMapper.readValue(line, new TypeReference<>() {
        });
    }

    private void assertRowsMatch(List<Map<String, Object>> rows, List<TestEvaluationModel> expected) {
        for (int i = 0; i < expected.size(); i++) {
            TestEvaluationModel model = expected.get(i);
            Map<String, Object> row = rows.get(i);

            assertThat(row.get("fullyQualifiedName")).isEqualTo(model.fullyQualifiedName());
            assertThat(((Number) row.get("startLine")).intValue()).isEqualTo(model.startLine());
            assertThat(row.get("label")).isEqualTo(model.label());
        }
    }

    private record TestEvaluationModel(String fullyQualifiedName, Integer startLine, String label) implements EvaluationModel {

        @Override
        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        @Override
        public Integer getStartLine() {
            return startLine;
        }
    }

    private record TestDto(String fullyQualifiedName, Integer startLine, String label) implements BaseDto {
    }

    private static class TestModelMapper implements ModelDtoMapper<TestEvaluationModel, TestDto> {

        @Override
        public TestDto toDto(TestEvaluationModel model) {
            return new TestDto(model.fullyQualifiedName(), model.startLine(), model.label());
        }
    }

}



