package cz.muni.jena.codeminer.outputformatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtractorOutputConfiguration {

    @Bean(destroyMethod = "")
    public OutputFormatter jsonOutputFormatter(ObjectMapper objectMapper) {
        return new JsonOutput(objectMapper);
    }
}
