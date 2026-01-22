package com.example.antipatterns.framework_coupling_and_multiple_forms_of_injection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MultipleFormsOfInjectionGreetingController
{
    @Autowired
    private GreetingService greeting;

    @Autowired
    public MultipleFormsOfInjectionGreetingController(GreetingService greeting)
    {
        this.greeting = greeting;
    }

    @GetMapping("/greeting/multiple/forms/injection")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return greeting.greeting(name);
    }

    @Autowired
    public void setGreeting(GreetingService greeting)
    {
        this.greeting = greeting;
    }
}
