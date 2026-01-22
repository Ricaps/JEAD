package com.example.antipatterns.complex_producer_method;

import com.example.antipatterns.AntiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class GreetingServiceFactory
{
    @Bean
    public GreetingService getGreetingService(ApplicationContext applicationContext)
    {
        Logger logger = LoggerFactory.getLogger(AntiPatterns.class);
        String applicationName = Optional.ofNullable(applicationContext.getId()).orElse("");
        logger.atInfo().log("We are doing random stuff to increase cyclomatic complexity of this method.");
        if (applicationName.equals("antipatterns"))
        {
            logger.atInfo().log("Really this application name is antipatterns?");
        } else
        {
            logger.atInfo().log("I am so surprised this application is not named antipatterns!");
            for (char character : applicationName.toCharArray())
            {
                logger.atInfo().log(String.valueOf(character));
            }
        }
        return new GreetingServiceImpl();
    }
}
