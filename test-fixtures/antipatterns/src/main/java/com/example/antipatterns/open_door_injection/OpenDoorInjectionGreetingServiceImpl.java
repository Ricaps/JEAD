package com.example.antipatterns.open_door_injection;

import org.springframework.stereotype.Service;

@Service
public class OpenDoorInjectionGreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";

    @Override
    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
