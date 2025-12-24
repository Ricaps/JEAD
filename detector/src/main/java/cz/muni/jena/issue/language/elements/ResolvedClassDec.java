package cz.muni.jena.issue.language.elements;

import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public record ResolvedClassDec(ResolvedClassDeclaration resolvedClassDeclaration)
{
    public List<ResolvedClassDeclaration> findAllClassesInHierarchy()
    {
        return Stream.concat(
                resolvedClassDeclaration.getAllSuperClasses()
                        .stream()
                        .map(
                                resolvedReferenceType -> resolvedReferenceType
                                        .getTypeDeclaration()
                                        .filter(ResolvedReferenceTypeDeclaration::isClass)
                                        .map(ResolvedReferenceTypeDeclaration::asClass)
                        )
                        .flatMap(Optional::stream),
                Stream.of(resolvedClassDeclaration)
        ).toList();
    }

    public boolean isAnyOfTheseClassesInHierarchy(Set<String> classes)
    {
        return findAllClassesInHierarchy()
                .stream()
                .map(ResolvedClassDeclaration::getQualifiedName)
                .anyMatch(classes::contains);
    }

    public List<ResolvedFieldDeclaration> getAllFields() {
        try {
            return resolvedClassDeclaration.getAllFields();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }
}
