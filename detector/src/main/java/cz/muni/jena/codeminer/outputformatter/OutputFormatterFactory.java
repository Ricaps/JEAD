package cz.muni.jena.codeminer.outputformatter;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Component
public class OutputFormatterFactory {

    private final Map<String, OutputFormatter> outputFormatterMap;

    @Inject
    public OutputFormatterFactory(Map<String, OutputFormatter> outputFormatterMap) {
        this.outputFormatterMap = outputFormatterMap;
    }

    /**
     * Returns code serializer based on the desired format
     * @param format format of the output, e.g. json
     * @return CodeSerializer wrapped in the Optional
     */
    public Optional<OutputFormatter> getCodeSerializer(@Nonnull String format) {
        return Optional.ofNullable(outputFormatterMap.get(getBeanName(format)));
    }

    private static String getBeanName(String format) {
        return format + "OutputFormatter";
    }
}
