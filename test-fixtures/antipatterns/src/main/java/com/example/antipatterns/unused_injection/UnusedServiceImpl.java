package com.example.antipatterns.unused_injection;

import org.springframework.stereotype.Service;

@Service
public class UnusedServiceImpl implements UnusedService
{
    @Override
    public String unusedMethod() {
        return null;
    }

    @Override
    public String unusedMethod2()
    {
        return null;
    }

    @Override
    public String unusedMethod3()
    {
        return null;
    }

    @Override
    public String unusedMethod4()
    {
        return null;
    }
}
