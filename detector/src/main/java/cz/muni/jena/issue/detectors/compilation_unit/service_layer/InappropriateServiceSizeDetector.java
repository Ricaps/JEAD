package cz.muni.jena.issue.detectors.compilation_unit.service_layer;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.Annotation;
import cz.muni.jena.configuration.service_layer.ServiceLayerConfiguration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueCategory;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.detectors.compilation_unit.SpecificIssueDetector;
import cz.muni.jena.issue.language.elements.ResolvableNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Stream;

@Component("inappropriateServiceSizeDetector")
public class InappropriateServiceSizeDetector implements SpecificIssueDetector
{
    @Override
    public @NonNull Stream<Issue> findIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration)
    {
        ServiceLayerConfiguration serviceLayerConfiguration = configuration.serviceLayerConfiguration();
        return findIssues(
                classOrInterfaceDeclaration,
                serviceLayerConfiguration.minServiceMethods(),
                serviceLayerConfiguration.maxServiceMethods(),
                serviceLayerConfiguration.serviceAnnotations()
        );
    }

    protected @NonNull Stream<Issue> findIssues(
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            int minServiceMethods,
            int maxServiceMethods,
            Set<Annotation> serviceAnnotations)
    {
        if (
                classOrInterfaceDeclaration.getAnnotations()
                        .stream()
                        .flatMap(ResolvableNode::resolve)
                        .map(ResolvedAnnotationDeclaration::getQualifiedName)
                        .noneMatch(annotation -> serviceAnnotations.stream().map(Annotation::fullyQualifiedName).anyMatch(annotation::equals))
        )
        {
            return Stream.of();
        }
        long methodCount = classOrInterfaceDeclaration.getMethods()
                .stream()
                .filter(NodeWithPublicModifier::isPublic)
                .count();
        if (methodCount > maxServiceMethods)
        {
            return Stream.of(Issue.fromClass(IssueType.MULTI_SERVICE, classOrInterfaceDeclaration));
        }
        if (methodCount < minServiceMethods)
        {
            return Stream.of(Issue.fromClass(IssueType.TINY_SERVICE, classOrInterfaceDeclaration));
        }
        return Stream.of();
    }

    @Override
    public @NonNull IssueCategory getIssueCategory()
    {
        return IssueCategory.SERVICE_LAYER;
    }
}
