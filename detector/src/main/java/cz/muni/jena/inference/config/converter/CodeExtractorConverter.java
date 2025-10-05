package cz.muni.jena.inference.config.converter;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;

@Component
@ConfigurationPropertiesBinding
public class CodeExtractorConverter implements Converter<String, CodeExtractor<?>> {

    private final List<CodeExtractor<?>> codeExtractors;

    public CodeExtractorConverter(List<CodeExtractor<?>> codeExtractors) {
        this.codeExtractors = codeExtractors;
    }

    @Override
    public CodeExtractor<?> convert(@Nullable String extractorName) {
        if (extractorName == null) {
            throw new IllegalArgumentException("Extractor name has to be defined!");
        }

        return codeExtractors.stream()
                .filter(codeExtractors -> codeExtractors.getIdentifier().equals(extractorName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Extractor with name %s doesn't exist!".formatted(extractorName)));
    }
}
