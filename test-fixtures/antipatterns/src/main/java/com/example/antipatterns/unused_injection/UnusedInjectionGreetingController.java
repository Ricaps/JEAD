package com.example.antipatterns.unused_injection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.inject.Inject;

@RestController
public class UnusedInjectionGreetingController
{
    private final GreetingService greeting;
    private final UnusedService unusedService;

    @Inject
    public UnusedInjectionGreetingController(GreetingService greeting, UnusedService unusedService)
    {
        this.greeting = greeting;
        this.unusedService = unusedService;
    }

    @GetMapping("unused_injection/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return greeting.greeting(name);
    }
}
