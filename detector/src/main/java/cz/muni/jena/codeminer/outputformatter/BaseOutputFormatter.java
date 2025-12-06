package cz.muni.jena.codeminer.outputformatter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class BaseOutputFormatter implements OutputFormatter {
    private String outputPath;

    @Override
    public void setOutputPath(String path) {
        this.outputPath = path;
    }

    protected final OutputStream getOutputStream() {
        return getOutputStream(false);
    }

    protected final OutputStream getOutputStream(boolean append) {
        if (this.outputPath == null) {
            throw new IllegalStateException("Output file was not provided!");
        }

        StandardOpenOption openOption = append ? StandardOpenOption.APPEND : StandardOpenOption.WRITE;

        try {
            return Files.newOutputStream(Path.of(outputPath), StandardOpenOption.CREATE, openOption);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
