package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;

public interface MetricComputer<T> {

    T extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration);

    /**
     * Returns property name of the processed metric, which can be then used in object mapping to concrete property
     * @return property name of current metric
     */
    String getPropertyName();

}
