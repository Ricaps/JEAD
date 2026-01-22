package com.example.antipatterns.concrete_class_injection;

import org.springframework.stereotype.Service;

@Service
public class GreetingServiceImpl
{
    private static final String TEMPLATE = "Hello, %s!";

    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
