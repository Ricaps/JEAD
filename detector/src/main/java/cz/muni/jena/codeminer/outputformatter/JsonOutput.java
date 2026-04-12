package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistry;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonOutput extends BaseOutputFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonOutput.class);

    private final ObjectMapper objectMapper;
    private final List<Object> buffer = new ArrayList<>();

    public JsonOutput(ObjectMapper objectMapper, ModelMapperRegistry modelMapperRegistry) {
        super(modelMapperRegistry);
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(@Nonnull List<? extends EvaluationModel> codeSnippets) {
        if (codeSnippets.isEmpty()) {
            return;
        }

        var dtoSnippets = mapToDto(codeSnippets);

        synchronized (buffer) {
            buffer.addAll(dtoSnippets);
        }
    }

    @Override
    public void close() {
        synchronized (buffer) {

            if (buffer.isEmpty()) {
                return;
            }

            try (OutputStream outputStream = getOutputStream()) {
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
}
