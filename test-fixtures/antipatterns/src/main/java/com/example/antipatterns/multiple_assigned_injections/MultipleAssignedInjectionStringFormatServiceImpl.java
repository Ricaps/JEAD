package com.example.antipatterns.multiple_assigned_injections;

import org.springframework.stereotype.Service;

@Service
public class MultipleAssignedInjectionStringFormatServiceImpl implements StringFormatService
{
    @Override
    public String format(String name, String template) {
        return String.format(template, name);
    }
}
