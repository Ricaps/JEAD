package com.example.antipatterns.open_window_injection;

public interface GreetingService
{
    String greeting(String name, DefaultValueService defaultValueService);
}
