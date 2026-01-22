package com.example.antipatterns.open_window_injection;

public interface DefaultValueService
{
    <T> T returnNotNull(T value, T defaultValue);
}
