package cz.muni.jena.codeminer.extractor.god_di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import cz.muni.jena.codeminer.EvaluatedNode;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.codeminer.extractor.god_di.metrics.MetricComputer;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.Annotation;
import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.issue.language.elements.ResolvableNode;
import cz.muni.jena.util.NodeUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GodDIMetricsExtractor extends BaseCodeExtractor<DIMetricsDto> {

    private static final String GOD_DI_METRIC_EXTRACTOR = "god-metrics";
    private final List<MetricComputer<?>> metricComputers;
    private final ObjectMapper objectMapper;

    @Inject
    protected GodDIMetricsExtractor(List<MetricComputer<?>> metricComputers, ObjectMapper objectMapper) {
        super(GOD_DI_METRIC_EXTRACTOR);
        this.metricComputers = metricComputers;
        this.objectMapper = objectMapper;
    }

    @Override
    public Stream<DIMetricsDto> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration) {
        if (!containsAnnotation(classOrInterface, configuration.diConfiguration())) {
            return Stream.empty();
        }

        Map<String, Object> objectValueMap = metricComputers.stream()
                .map(metricComputer -> {
                    var metric = metricComputer.extractMetric(classOrInterface, configuration);

                    return Map.entry(metricComputer.getPropertyName(), metric);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        objectValueMap.put("evaluatedNode", getEvaluatedNode(classOrInterface));
        objectValueMap.put("code", getCode(classOrInterface));

        return Stream.of(objectMapper.convertValue(objectValueMap, DIMetricsDto.class));
    }

    private EvaluatedNode getEvaluatedNode(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return new EvaluatedNodeProvider(classOrInterfaceDeclaration.getFullyQualifiedName().orElse(null), NodeUtil.getStartLineNumber(classOrInterfaceDeclaration).orElse(null));
    }

    private String getCode(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.toString();
    }

    private boolean containsAnnotation(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, DIConfiguration diConfiguration) {
        List<String> configuredAnnotations = Stream.concat(diConfiguration.injectionAnnotations().stream(), diConfiguration.beanAnnotations().stream()).map(Annotation::fullyQualifiedName).toList();

        return classOrInterfaceDeclaration.findAll(AnnotationExpr.class)
                .stream()
                .flatMap(ResolvableNode::resolve)
                .map(ResolvedAnnotationDeclaration::getQualifiedName)
                .anyMatch(configuredAnnotations::contains);
    }
}
