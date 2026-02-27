package cz.muni.jena.issue.language.elements;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import cz.muni.jena.configuration.di.Annotation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Class(ClassOrInterfaceDeclaration classOrInterfaceDeclaration)
{
    public Stream<FieldDeclaration> findInjectedFields(List<Annotation> injectionAnnotations)
    {
        List<FieldDeclaration> fields = classOrInterfaceDeclaration.findAll(FieldDeclaration.class);
        Stream<? extends CallableDeclaration<? extends CallableDeclaration<?>>> callableDeclarations = Stream.concat(
                findInjectableConstructors(injectionAnnotations),
                classOrInterfaceDeclaration.getMethods().stream()
        );
        List<CallableDec<CallableDeclaration<?>>> injectionCallables = callableDeclarations
                .map(constructorDeclaration -> new CallableDec<CallableDeclaration<?>>(constructorDeclaration, injectionAnnotations))
                .toList();

        return fields.stream().filter(
                field -> injectionCallables.stream()
                        .anyMatch(
                                constructor -> constructor.isInjectedHere(field)
                                        || new NodeWithAnnotation<>(field).hasAnyOfTheseAnnotations(injectionAnnotations)
                        )
        );
    }

    private Stream<? extends CallableDeclaration<? extends CallableDeclaration<?>>> findInjectableConstructors(List<Annotation> injectionAnnotations) {
        long constructorsCount = classOrInterfaceDeclaration.getConstructors().size();
        if (constructorsCount == 0) {
            return Stream.empty();
        }
        if (constructorsCount == 1) {
            return classOrInterfaceDeclaration.getConstructors().stream();
        }
        return classOrInterfaceDeclaration.getConstructors().stream()
                .filter(constructor -> new NodeWithAnnotation<>(constructor).hasAnyOfTheseAnnotations(injectionAnnotations));
    }

    public Stream<ReturnStmt> findReturnStatementsLeakingInjectedFields(Collection<String> injectedFieldsNames)
    {
        return classOrInterfaceDeclaration.findAll(ReturnStmt.class)
            .stream()
            .map(ReturnStatement::new)
            .filter(returnStatement -> returnStatement.isInjectedFieldReturned(injectedFieldsNames))
            .map(ReturnStatement::getReturnStmt);
    }

    public Stream<NodeWithRange<?>> findParametersAssignedIntoMultipleFields(
            Set<String> injectedFieldsNames,
            List<Annotation> injectionAnnotations
    )
    {
        List<? extends CallableDeclaration<? extends CallableDeclaration<?>>> methods = Stream.concat(
                classOrInterfaceDeclaration.findAll(MethodDeclaration.class).stream(),
                classOrInterfaceDeclaration.findAll(ConstructorDeclaration.class).stream()
        ).toList();

        return methods.stream()
                .map(method -> new CallableDec<>(method, injectionAnnotations))
                .flatMap(
                        method -> Stream.of(method)
                                .map(CallableDec::findInjectedFields)
                                .map(injectedFields -> injectedFields.flatMap(
                                        entry -> Stream.of(Map.entry(new ParameterWrapper(entry.getKey()), entry.getValue()))
                                ))
                                .map(injectedFields -> injectedFields.collect(Collectors.toMap(
                                        Map.Entry<ParameterWrapper, ResolvedFieldDeclaration>::getKey,
                                        entry -> Stream.of(entry.getValue()),
                                        Stream::concat
                                )))
                                .map(Map::entrySet)
                                .map(entries -> entries.stream().map(
                                        entry -> Map.entry(entry.getKey(), entry.getValue().toList())
                                     ).filter(entry -> entry.getValue().size() > 1)
                                ).flatMap(
                                        entries -> {
                                            Set<String> parametersWithMultipleOccurrences = entries.map(Map.Entry::getKey).map(ParameterWrapper::parameter)
                                                    .map(Parameter::getName)
                                                    .map(SimpleName::asString)
                                                    .collect(Collectors.toSet());
                                            return method.callableDeclaration().findAll(AssignExpr.class)
                                                    .stream()
                                                    .map(AssignExpression::new)
                                                    .flatMap(assignExpression -> assignExpression.findFieldAssigmentOfInjectedField(injectedFieldsNames))
                                                    .filter(assignExpr -> parametersWithMultipleOccurrences.contains(assignExpr.getValue().asNameExpr().getName().asString()));
                                        }
                                )

                );
    }

    public Stream<NodeWithRange<?>> findCallsLeakingInjectedFields(
            Collection<String> injectedFieldsNames,
            List<Annotation> injectionAnnotations
    )
    {
        Stream<? extends CallableDeclaration<? extends CallableDeclaration<?>>> methods = Stream.concat(
                classOrInterfaceDeclaration.findAll(MethodDeclaration.class).stream(),
                classOrInterfaceDeclaration.findAll(ConstructorDeclaration.class).stream()
        );

        return methods
                .map(method -> new CallableDec<>(method, injectionAnnotations))
                .flatMap(callable -> Stream.of(callable.callableDeclaration())
                        .flatMap(
                                method -> Stream.of(
                                                method.findAll(MethodCallExpr.class).stream(),
                                                method.findAll(ObjectCreationExpr.class).stream(),
                                                method.findAll(ExplicitConstructorInvocationStmt.class).stream()
                                        )
                                        .flatMap(i -> i)
                        )
                        .map(ResolvableNodeWithArguments::new)
                        .filter(resolvableNodeWithArguments -> !resolvableNodeWithArguments.belongsToClass(
                                classOrInterfaceDeclaration))
                        .filter(
                                resolvableNodeWithArguments -> resolvableNodeWithArguments.hasInjectedFieldParameter(
                                        injectedFieldsNames)
                                        || resolvableNodeWithArguments.hasParameterAssignedIntoInjectedField(
                                        callable.findInjectedFields()
                                                .map(Map.Entry::getKey)
                                                .collect(Collectors.toSet())
                                )
                        )
                        .map(ResolvableNodeWithArguments::resolvableNodeWithArguments)
                );
    }

    public static boolean isFinalClass(Stream<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclaration)
    {
        return resolvedReferenceTypeDeclaration.filter(ResolvedReferenceTypeDeclaration::isClass)
                .map(ResolvedReferenceTypeDeclaration::asClass)
                .filter(resolvedClassDeclaration1 -> !resolvedClassDeclaration1.isAnonymousClass())
                .map(resolvedClassDeclaration -> resolvedClassDeclaration.toAst(ClassOrInterfaceDeclaration.class))
                .flatMap(Optional::stream)
                .anyMatch(ClassOrInterfaceDeclaration::isFinal);
    }
}
