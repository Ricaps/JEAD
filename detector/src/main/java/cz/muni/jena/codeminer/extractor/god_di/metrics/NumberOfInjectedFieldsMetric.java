package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.language.elements.Class;
import org.springframework.stereotype.Component;

@Component
public class NumberOfInjectedFieldsMetric implements MetricComputer<Long>{

    public static final String PROPERTY_NAME = "injectedFields";

    @Override
    public Long extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        return new Class(classOrInterfaceDeclaration).findInjectedFields(configuration.diConfiguration().injectionAnnotations()).count();
    }

    @Override
    public String getPropertyName() {
        return PROPERTY_NAME;
    }
}
