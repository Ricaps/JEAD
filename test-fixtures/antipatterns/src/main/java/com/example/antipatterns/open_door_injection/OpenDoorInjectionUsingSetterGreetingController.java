package com.example.antipatterns.open_door_injection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class OpenDoorInjectionUsingSetterGreetingController
{
    private GreetingService greeting;

    @GetMapping("open_door_injection_with_injection_setter/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return greeting.greeting(name);
    }

    @Inject
    public void setGreeting(GreetingService greeting)
    {
        this.greeting = greeting;
    }
}
