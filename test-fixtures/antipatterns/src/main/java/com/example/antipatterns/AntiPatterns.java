package com.example.antipatterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AntiPatterns
{
    private static final Logger logger;
    static {
        logger = LoggerFactory.getLogger(AntiPatterns.class);
        logger.atInfo().log("This is static block.");
        logger.atInfo().log("It can span multiple lines.");
        logger.atInfo().log("And people sometimes use it to initialise static fields.");
        try
        {
            loadConfiguration();
        } catch (Exception ignored)
        {

        }
    }

    public final void init() throws CustomIOException
    {
    }

    public static String helloWorld()
    {
        return "Hello world!";
    }

    public AntiPatterns() throws CustomIOException
    {
    }

    public static void loadConfiguration() throws CustomIOException
    {
        //It is irrelevant what is inside the method. The important thing is that it has the exception in signature.
    }

    public static void main(String[] args) throws CustomIOException
    {
        AntiPatterns antiPatterns = new AntiPatterns();
        antiPatterns.init();
        AntiPatterns.loadConfiguration();
        new FinalClass().idk();
        SpringApplication.run(AntiPatterns.class, args);
    }

}
