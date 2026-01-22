package com.example.antipatterns.multiple_assigned_injections;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class MultipleAssignedInjectionGreetingController extends MultipleAssignedInjectionGreetingControllerParent
{
    private final StringFormatService stringFormatService;
    private final StringFormatService stringFormatService1;

    @Inject
    public MultipleAssignedInjectionGreetingController(StringFormatService stringFormatService)
    {
        super(stringFormatService);
        this.stringFormatService = stringFormatService;
        stringFormatService1 = stringFormatService;
    }

    @Override
    @GetMapping("multiple_assigned_injections/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name)
    {
        return stringFormatService.format(super.greeting(name), "How are things going?");
    }
}
