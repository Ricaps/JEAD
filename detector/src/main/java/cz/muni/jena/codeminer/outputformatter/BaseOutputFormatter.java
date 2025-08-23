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
        if (this.outputPath == null) {
            throw new IllegalStateException("Output file was not provided!");
        }

        try {
            return Files.newOutputStream(Path.of(outputPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
