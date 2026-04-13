package cz.muni.jena.codeminer.outputformatter;

import cz.muni.jena.inference.model.mapping.ModelMapperRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;

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

    @Bean
    public CsvMapper csvMapper() {
        return new CsvMapper();
    }

    @Bean(destroyMethod = "")
    public OutputFormatterInstanceProvider jsonOutputFormatter(ObjectMapper objectMapper, ModelMapperRegistry modelMapperRegistry) {
        return () -> new JsonOutput(objectMapper, modelMapperRegistry);
    }

    @Bean(destroyMethod = "", name = "jsonlOutputFormatter")
    public OutputFormatterInstanceProvider jsonLinesOutputFormatter(ObjectMapper objectMapper, ModelMapperRegistry modelMapperRegistry) {
        return () -> new JsonLinesOutput(objectMapper, modelMapperRegistry);
    }

    @Bean(destroyMethod = "", name = "csvOutputFormatter")
    public OutputFormatterInstanceProvider csvOutputFormatter(CsvMapper csvMapper, ObjectMapper objectMapper, ModelMapperRegistry modelMapperRegistry) {
        return () -> new CsvOutput(csvMapper, objectMapper, modelMapperRegistry);
    }
}
