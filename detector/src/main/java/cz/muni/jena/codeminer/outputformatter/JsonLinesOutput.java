package cz.muni.jena.codeminer.outputformatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonLinesOutput extends BaseOutputFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLinesOutput.class);
    private static final int BUFFER_SIZE = 30;
    private final ObjectMapper objectMapper;
    private final List<Object> buffer = new ArrayList<>();

    public JsonLinesOutput(ObjectMapper objectMapper) {
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

    private void flush() {
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }

            try (OutputStream outputStream = getOutputStream(true);
                 SequenceWriter seqWriter = objectMapper.writer().withRootValueSeparator("\n").writeValues(outputStream);
            ) {
                seqWriter.writeAll(buffer);
            } catch (IOException e) {
                LOGGER.error("Failed to append to JSON lines output", e);
            } finally {
                buffer.clear();
            }
        }
    }
}
