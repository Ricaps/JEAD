package cz.muni.jena.configuration;

import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.configuration.mocking.MockingConfiguration;
import cz.muni.jena.configuration.persistence.PersistenceConfiguration;
import cz.muni.jena.configuration.security.SecurityConfiguration;
import cz.muni.jena.configuration.service_layer.ServiceLayerConfiguration;

public record Configuration(
        DIConfiguration diConfiguration,
        MockingConfiguration mockingConfiguration,
        SecurityConfiguration securityConfiguration,
        ServiceLayerConfiguration serviceLayerConfiguration,
        PersistenceConfiguration persistenceConfiguration
)
{

}
