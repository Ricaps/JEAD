package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.configuration.Configuration;
import org.springframework.stereotype.Component;

@Component
public class LinesOfCodeMetric implements MetricComputer<Integer> {

    private static final String PROPERTY_NAME = "linesOfCode";

    @Override
    public Integer extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        return classOrInterfaceDeclaration.getRange().map(range -> range.end.line - range.begin.line + 1).orElse(null);
    }

    @Override
    public String getPropertyName() {
        return PROPERTY_NAME;
    }
}
