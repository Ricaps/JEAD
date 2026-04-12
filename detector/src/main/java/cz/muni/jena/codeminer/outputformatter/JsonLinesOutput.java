package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SequenceWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonLinesOutput extends BaseOutputFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLinesOutput.class);
    private static final int BUFFER_SIZE = 30;
    private final ObjectMapper objectMapper;
    private final List<Object> buffer = new ArrayList<>();

    public JsonLinesOutput(ObjectMapper objectMapper, ModelMapperRegistry modelMapperRegistry) {
        super(modelMapperRegistry);
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(List<? extends EvaluationModel> codeSnippets) {
        if (codeSnippets.isEmpty()) {
            return;
        }

        var dtoSnippets = mapToDto(codeSnippets);

        synchronized (buffer) {
            buffer.addAll(dtoSnippets);

            if (buffer.size() > BUFFER_SIZE) {
                flush();
            }
        }
    }

    @Override
    public void close() {
        flush();
    }

    private void flush() {
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }

            try (OutputStream outputStream = getOutputStream(true);
                 SequenceWriter seqWriter = objectMapper.writer().withRootValueSeparator("\n").writeValues(outputStream)
            ) {
                seqWriter.writeAll(buffer);
                outputStream.write("\n".getBytes());
            } catch (IOException e) {
                LOGGER.error("Failed to append to JSON lines output", e);
            } finally {
                buffer.clear();
            }
        }
    }
}
