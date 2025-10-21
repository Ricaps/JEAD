package cz.muni.jena;

import cz.muni.jena.inference.config.InferenceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@ConfigurationPropertiesScan
@EnableConfigurationProperties(InferenceConfiguration.class)
@CommandScan
public class App
{
    public static void main(String[] args)
    {
        SpringApplication.run(App.class, args);
    }
}
