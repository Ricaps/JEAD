package cz.muni.jena.codeminer.outputformatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Component("jsonOutputFormatter")
public class JsonOutput extends BaseOutputFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonOutput.class);

    private final ObjectMapper objectMapper;
    private final List<Object> buffer = new ArrayList<>();

    public JsonOutput(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(@Nonnull List<Object> codeSnippets) {
        if (codeSnippets.isEmpty()) {
            return;
        }
        buffer.addAll(codeSnippets);
    }

    @Override
    public void close() {
        try (OutputStream outputStream = getOutputStream()) {
            buffer.add("// \"test\"");
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(outputStream, buffer);
        } catch (IOException e) {
            LOGGER.error("Failed to write JSON output to file", e);
        } finally {
            buffer.clear();
        }
    }
}
