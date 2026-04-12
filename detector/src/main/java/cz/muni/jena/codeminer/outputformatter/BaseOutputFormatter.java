package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.dto.BaseDto;
import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.inference.model.mapping.ModelDtoMapper;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistry;
import cz.muni.jena.inference.model.mapping.ModelMapperRegistryConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;

public abstract class BaseOutputFormatter implements OutputFormatter {

    private final ModelMapperRegistry modelMapperRegistry;
    private String outputPath;

    protected BaseOutputFormatter(ModelMapperRegistry modelMapperRegistry) {
        this.modelMapperRegistry = modelMapperRegistry;
    }

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

    @SuppressWarnings("unchecked")
    protected final Collection<? extends BaseDto> mapToDto(List<? extends EvaluationModel> codeSnippets) {
        if (codeSnippets.isEmpty()) {
            return List.of();
        }

        var mapper = (ModelDtoMapper<EvaluationModel, BaseDto>) modelMapperRegistry.getMapper(codeSnippets.getFirst().getClass());

        return codeSnippets.stream()
                .map(mapper::toDto)
                .toList();
    }

    protected String getOutputPath() {
        return this.outputPath;
    }
}
