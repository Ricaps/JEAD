package com.example.antipatterns.complex_producer_method;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class ComplexProducerMethodGreetingController
{
    private final GreetingService greeting;

    @Inject
    public ComplexProducerMethodGreetingController(GreetingService greeting)
    {
        this.greeting = greeting;
    }

    @GetMapping("complex_producer_method/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return greeting.greeting(name);
    }
}
