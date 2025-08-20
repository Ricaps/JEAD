package cz.muni.jena.codeminer.outputformatter;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class OutputFormatterFactory {

    private static final Map<String, ? extends OutputFormatter> codeSerializerMapping = Map.of(
        "json", new JsonOutput()
    );

    /**
     * Returns code serializer based on the desired format
     * @param format format of the output, e.g. json
     * @return CodeSerializer wrapped in the Optional
     */
    public Optional<OutputFormatter> getCodeSerializer(@Nonnull String format) {
        return Optional.of(codeSerializerMapping.get(format));
    }

    /**
     * Returns possible formats as readable string delimited by ', '
     * @return readable String
     */
    public static String getPossibleFormatsAsString() {
        return String.join(", ", codeSerializerMapping.keySet());
    }
}
