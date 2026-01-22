package com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection;

import org.springframework.stereotype.Service;

@Service
public class MultipleFormsOfInjectionGreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";

    @Override
    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
