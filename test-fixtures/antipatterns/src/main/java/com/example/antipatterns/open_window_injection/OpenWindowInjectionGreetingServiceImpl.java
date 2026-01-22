package com.example.antipatterns.open_window_injection;

import org.springframework.stereotype.Service;

@Service
public class OpenWindowInjectionGreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";
    private static final String DEFAULT_VALUE = "world";

    @Override
    public String greeting(String name, DefaultValueService defaultValueService) {
        return String.format(TEMPLATE, defaultValueService.returnNotNull(name, DEFAULT_VALUE));
    }
}
