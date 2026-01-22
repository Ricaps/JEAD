package com.example.antipatterns.complex_producer_method;

public class GreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";

    @Override
    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
