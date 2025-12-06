package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.language.elements.NodeWrapper;
import org.springframework.stereotype.Component;

@Component
public class CyclomaticComplexityMetric implements MetricComputer<Long> {

    public static final String PROPERTY_NAME = "cyclomaticComplexity";

    @Override
    public Long extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        return new NodeWrapper<>(classOrInterfaceDeclaration).calculateComplexity();
    }

    @Override
    public String getPropertyName() {
        return PROPERTY_NAME;
    }
}
