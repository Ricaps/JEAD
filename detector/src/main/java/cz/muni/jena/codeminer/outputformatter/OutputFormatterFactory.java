package cz.muni.jena.codeminer.outputformatter;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Component
public class OutputFormatterFactory {

    private final Map<String, OutputFormatterInstanceProvider> instanceProviderMap;

    @Inject
    public OutputFormatterFactory(Map<String, OutputFormatterInstanceProvider> instanceProviderMap) {
        this.instanceProviderMap = instanceProviderMap;
    }

    /**
     * Returns code serializer based on the desired format
     * @param format format of the output, e.g. json
     * @return CodeSerializer wrapped in the Optional
     */
    public Optional<OutputFormatter> getCodeSerializer(@Nonnull String format) {
        return Optional.ofNullable(instanceProviderMap.get(getBeanName(format)).createInstance());
    }

    private static String getBeanName(String format) {
        return format + "OutputFormatter";
    }
}
