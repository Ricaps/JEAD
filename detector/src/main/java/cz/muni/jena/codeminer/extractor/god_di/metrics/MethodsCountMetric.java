package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import cz.muni.jena.configuration.Configuration;
import org.springframework.stereotype.Component;

@Component
public class MethodsCountMetric implements MetricComputer<Integer> {

    private static final String PROPERTY_NAME = "methodsCount";

    @Override
    public Integer extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        return classOrInterfaceDeclaration
                .findAll(MethodDeclaration.class)
                .stream()
                .filter(method -> !method.isStatic())
                .toList()
                .size();
    }

    @Override
    public String getPropertyName() {
        return PROPERTY_NAME;
    }
}
