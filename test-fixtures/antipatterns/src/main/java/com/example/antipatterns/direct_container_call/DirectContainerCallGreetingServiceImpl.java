package com.example.antipatterns.direct_container_call;

import org.springframework.stereotype.Service;

@Service
public class DirectContainerCallGreetingServiceImpl implements GreetingService
{
    private static final String TEMPLATE = "Hello, %s!";

    @Override
    public String greeting(String name) {
        return String.format(TEMPLATE, name);
    }
}
