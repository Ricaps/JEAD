package cz.muni.jena.codeminer.outputformatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OutputConfig {

    @Bean
    public ObjectMapper objectMapper() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        return JsonMapper.builder()
                .defaultPrettyPrinter(prettyPrinter)
                .build();
    }

    @Bean(destroyMethod = "")
    public OutputFormatter jsonOutputFormatter(ObjectMapper objectMapper) {
        return new JsonOutput(objectMapper);
    }

    @Bean(destroyMethod = "", name = "jsonlOutputFormatter")
    public OutputFormatter jsonLinesOutputFormatter(ObjectMapper objectMapper) {
        return new JsonLinesOutput(objectMapper);
    }
}
