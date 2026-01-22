package com.example.antipatterns.open_door_injection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class OpenDoorInjectionUsingConstructorGreetingController
{
    private GreetingService greeting;

    @Inject
    public OpenDoorInjectionUsingConstructorGreetingController(GreetingService greeting)
    {
        this.greeting = greeting;
    }

    @GetMapping("open_door_injection/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return greeting.greeting(name);
    }

    public void setGreeting(GreetingService greeting)
    {
        this.greeting = greeting;
    }
}
