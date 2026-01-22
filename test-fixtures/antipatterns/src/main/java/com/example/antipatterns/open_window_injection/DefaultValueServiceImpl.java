package com.example.antipatterns.open_window_injection;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultValueServiceImpl implements DefaultValueService
{
    @Override
    public <T> T returnNotNull(T value, T defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }
}
