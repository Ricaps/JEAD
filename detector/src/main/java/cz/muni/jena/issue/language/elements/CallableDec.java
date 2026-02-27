package cz.muni.jena.issue.language.elements;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import cz.muni.jena.configuration.di.Annotation;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record CallableDec<T extends CallableDeclaration<? extends CallableDeclaration<?>>>(
        T callableDeclaration,
        List<Annotation> injectionAnnotations
)
{
    public boolean isThereConcreteClassInjection()
    {
        if (!isInjectionCallable())
        {
            return false;
        }
        return callableDeclaration.findAll(AssignExpr.class).stream().map(AssignExpression::new)
                .filter(assignExpression -> assignExpression.isFieldAssignedInCallable(callableDeclaration))
                .anyMatch(
                        assignExpression -> callableDeclaration.getParameterByName(assignExpression.assignExpr().getValue().toString())
                                .map(parameter -> Stream.of(parameter).anyMatch(ResolvableNode::isTypeDecClass))
                                .orElse(false)
                );
    }

    public static List<ResolvedType> getSpecifiedException(ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration)
    {
        try
        {
            return resolvedMethodLikeDeclaration.getSpecifiedExceptions();
        } catch (RuntimeException e)
        {
            return List.of();
        }
    }

    public Stream<Map.Entry<Parameter, ResolvedFieldDeclaration>> findInjectedFields()
    {
        if (!isInjectionCallable())
        {
            return Stream.of();
        }
        return callableDeclaration.findAll(AssignExpr.class).stream().map(AssignExpression::new)
                .flatMap(assignExpression -> assignExpression.findAssignedFieldsFromParameter(callableDeclaration));
    }

    public boolean isInjectedHere(FieldDeclaration field)
    {
        return ResolvableNode.resolve(field)
                .map(ResolvedFieldDeclaration::getName)
                .map(
                        fieldName -> callableDeclaration.findAll(AssignExpr.class)
                                .stream()
                                .map(AssignExpression::new)
                                .filter(assignExpression -> assignExpression.isFieldAssignedInCallable(
                                        callableDeclaration))
                                .filter(assignment -> assignment.resolveTargetField().getName().equals(fieldName))
                                .toList()
                )
                .anyMatch(fieldAssignments -> !fieldAssignments.isEmpty() && isInjectionCallable());
    }

    public boolean isInjectionCallable()
    {
        if (callableDeclaration.isConstructorDeclaration()) {
            // Constructors are often used for injection, so we consider them as injection callables by default
            return true;
        }

        return injectionAnnotations.stream()
                .anyMatch(
                        annotation -> callableDeclaration.getAnnotationByName(annotation.simpleName())
                                .isPresent()
                );
    }

}
