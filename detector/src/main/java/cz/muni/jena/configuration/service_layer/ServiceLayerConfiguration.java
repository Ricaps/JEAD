package cz.muni.jena.configuration.service_layer;

import cz.muni.jena.configuration.di.Annotation;

import java.util.Set;

public record ServiceLayerConfiguration(
        int maxServiceMethods,
        int minServiceMethods,
        Set<Annotation> serviceAnnotations
)
{
}
