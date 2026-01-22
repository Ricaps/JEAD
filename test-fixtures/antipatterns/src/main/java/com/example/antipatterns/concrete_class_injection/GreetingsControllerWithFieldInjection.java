package com.example.antipatterns.concrete_class_injection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class GreetingsControllerWithFieldInjection
{
    @Inject
    private GreetingServiceImpl greetingService;

    @GetMapping("/greeting/using/field/injection")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name)
    {
        return greetingService.greeting(name);
    }
}
