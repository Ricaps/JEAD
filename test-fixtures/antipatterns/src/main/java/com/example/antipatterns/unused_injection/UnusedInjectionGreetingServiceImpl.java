package com.example.antipatterns.unused_injection;

import org.springframework.stereotype.Service;

@Service
public class UnusedInjectionGreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";

    @Override
    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
