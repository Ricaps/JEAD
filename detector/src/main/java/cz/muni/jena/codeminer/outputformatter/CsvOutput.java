package cz.muni.jena.codeminer.outputformatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CsvOutput extends BaseOutputFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvOutput.class);
    private static final int BUFFER_SIZE = 30;
    private final List<Object> buffer = new ArrayList<>();
    private final CsvMapper csvMapper;
    private final ObjectMapper objectMapper;

    public CsvOutput(CsvMapper csvMapper, ObjectMapper objectMapper) {
        this.csvMapper = csvMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(Collection<?> codeSnippets) {
        if (codeSnippets.isEmpty()) {
            return;
        }
        synchronized (buffer) {
            buffer.addAll(codeSnippets);
            if (buffer.size() > BUFFER_SIZE) {
                flush();
            }
        }
    }

    @Override
    public void close() {
        flush();
    }

    private boolean isFileEmpty() {
        try {
            Path path = Path.of(getOutputPath());
            return !Files.exists(path) || Files.size(path) == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private void flush() {
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }
            List<Map<String, Object>> rows = buffer.stream()
                    .map(obj -> (Map<String, Object>) objectMapper.convertValue(obj, Map.class))
                    .toList();
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            rows.getFirst().keySet().forEach(schemaBuilder::addColumn);
            CsvSchema schema = isFileEmpty()
                    ? schemaBuilder.build().withHeader()
                    : schemaBuilder.build();
            try (OutputStream outputStream = getOutputStream(true);
                 SequenceWriter seqWriter = csvMapper.writerFor(Map.class).with(schema).writeValues(outputStream)
            ) {
                seqWriter.writeAll(rows);
                outputStream.write("\n".getBytes());
            } catch (IOException e) {
                LOGGER.error("Failed to append to CSV output", e);
            } finally {
                buffer.clear();
            }
        }
    }
}
