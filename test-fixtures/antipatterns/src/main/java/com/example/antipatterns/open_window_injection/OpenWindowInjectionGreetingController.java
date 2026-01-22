package com.example.antipatterns.open_window_injection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class OpenWindowInjectionGreetingController
{
    private final GreetingService greeting;
    private final DefaultValueService defaultValueService;

    @Inject
    public OpenWindowInjectionGreetingController(GreetingService greeting, DefaultValueService defaultValueService)
    {
        this.greeting = greeting;
        this.defaultValueService = defaultValueService;
    }

    @GetMapping("/open_window_injection/greeting")
    public String greeting(@RequestParam(value = "name", required = false) String name) {
        return greeting.greeting(name, defaultValueService);
    }

    public DefaultValueService getDefaultValueService()
    {
        return defaultValueService;
    }
}
