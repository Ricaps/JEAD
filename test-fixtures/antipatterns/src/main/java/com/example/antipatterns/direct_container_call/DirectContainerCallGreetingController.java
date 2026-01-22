package com.example.antipatterns.direct_container_call;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class DirectContainerCallGreetingController
{
    private final ApplicationContext context;

    @Inject
    public DirectContainerCallGreetingController(ApplicationContext context)
    {
        this.context = context;
    }

    @GetMapping("/direct/container/call/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return context.getBean(GreetingService.class).greeting(name);
    }
}
