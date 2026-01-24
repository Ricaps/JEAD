package cz.muni.jena;

import cz.muni.jena.configuration.prepare_plugin.PreparePluginConfig;
import cz.muni.jena.inference.config.InferenceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan
@EnableConfigurationProperties({InferenceConfiguration.class, PreparePluginConfig.class})
@CommandScan
public class App {
    static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
