package cz.muni.jena.codeminer.extractor.multi_service;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.codeminer.extractor.god_di.metrics.LackOfCohesionOfMethodsMetric;
import cz.muni.jena.codeminer.extractor.multi_service.model.MultiServiceMethods;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsMap;
import cz.muni.jena.issue.language.elements.NodeWithAnnotation;
import cz.muni.jena.issue.language.elements.NodeWrapper;
import cz.muni.jena.util.NodeUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class MultiServiceExtractor extends BaseCodeExtractor<MultiServiceMethods> {

    private static final String EXTRACTOR_IDENTIFIER = "multi-service";
    private static final List<AccessSpecifier> ACCESS_SPECIFIERS = List.of(AccessSpecifier.PUBLIC, AccessSpecifier.NONE);
    private final LackOfCohesionOfMethodsMetric lcomCalculator;

    protected MultiServiceExtractor(LackOfCohesionOfMethodsMetric lcomCalculator) {
        super(EXTRACTOR_IDENTIFIER);
        this.lcomCalculator = lcomCalculator;
    }

    @Override
    public Stream<MultiServiceMethods> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration, CommandSettingsMap commandSettingsMap) {
        NodeWrapper<ClassOrInterfaceDeclaration> nodeWithAnnotation = new NodeWrapper<>(classOrInterface);
        if (!nodeWithAnnotation.containsAnnotation(configuration.serviceLayerConfiguration().serviceAnnotations().stream().toList())) {
            return Stream.of();
        }

        int cohesion = lcomCalculator.extractMetric(classOrInterface, configuration);

        return Stream.of(classOrInterface).map(classOrInterfaceDeclaration -> {
            List<MultiServiceMethods.Method> methods = classOrInterfaceDeclaration.getMethods()
                    .stream()
                    .filter(methodDeclaration -> ACCESS_SPECIFIERS.contains(methodDeclaration.getAccessSpecifier()))
                    .map(this::clearSignature)
                    .map(methodDeclaration ->
                            new MultiServiceMethods.Method(methodDeclaration.getNameAsString(),
                                    methodDeclaration.getDeclarationAsString(false, false, true).trim())
                    ).toList();

            return new MultiServiceMethods(
                    methods,
                    cohesion,
                    classOrInterface.getFullyQualifiedName().orElse(""),
                    NodeUtil.getStartLineNumber(classOrInterface).orElse(0)
            );
        });
    }

    private MethodDeclaration clearSignature(MethodDeclaration declaration) {
        declaration.setAnnotations(new NodeList<>());
        declaration.getParameters().forEach(parameter -> {
            parameter.setAnnotations(new NodeList<>());
        });

        return declaration;
    }
}
