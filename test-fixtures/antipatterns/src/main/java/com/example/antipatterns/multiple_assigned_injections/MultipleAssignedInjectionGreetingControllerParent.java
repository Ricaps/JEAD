package com.example.antipatterns.multiple_assigned_injections;

public class MultipleAssignedInjectionGreetingControllerParent
{
    private static final String TEMPLATE = "Hello, %s! %s";
    private final StringFormatService stringFormatService;

    public MultipleAssignedInjectionGreetingControllerParent(StringFormatService stringFormatService)
    {
        this.stringFormatService = stringFormatService;
    }

    public String greeting(String name)
    {
        return stringFormatService.format(name, TEMPLATE);
    }
}
