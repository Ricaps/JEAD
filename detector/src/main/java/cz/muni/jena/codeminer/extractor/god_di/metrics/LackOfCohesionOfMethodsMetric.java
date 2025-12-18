package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.language.elements.ResolvableNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LackOfCohesionOfMethodsMetric implements MetricComputer<Integer> {

    private static void doDfs(String currentMethod, Set<String> visitedMethods, Map<String, Set<String>> methodsGraph) {
        visitedMethods.add(currentMethod);

        for (String nextMethod : methodsGraph.get(currentMethod)) {
            if (!visitedMethods.contains(nextMethod)) {
                doDfs(nextMethod, visitedMethods, methodsGraph);
            }
        }
    }

    private static void handleMethodCallsRelations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Map<String, Set<String>> methodsGraph) {
        classOrInterfaceDeclaration.getMethods()
                .forEach(callerMethod -> {
                    Optional<String> callerMethodSignature = ResolvableNode.resolveOptional(callerMethod).map(ResolvedMethodDeclaration::getSignature);

                    if (callerMethodSignature.isEmpty()) {
                        return;
                    }

                    callerMethod.findAll(MethodCallExpr.class).forEach(callExpression -> {
                        Optional<String> calleeMethodSignature = ResolvableNode.resolveOptional(callExpression).map(ResolvedMethodDeclaration::getSignature);

                        if (calleeMethodSignature.isPresent() && methodsGraph.containsKey(calleeMethodSignature.get())) {
                            methodsGraph.get(callerMethodSignature.get()).add(calleeMethodSignature.get());
                            methodsGraph.get(calleeMethodSignature.get()).add(callerMethodSignature.get());
                        }
                    });
                });
    }

    private static void handleMethodFieldsRelations(Map<String, Set<String>> methodsGraph, Map<String, Set<String>> methodToFields) {
        List<String> methodDeclarations = new ArrayList<>(methodsGraph.keySet());
        for (int i = 0; i < methodDeclarations.size(); i++) {
            for (int j = i + 1; j < methodDeclarations.size(); j++) {
                String methodDeclaration1 = methodDeclarations.get(i);
                String methodDeclaration2 = methodDeclarations.get(j);

                Set<String> fieldsInMethod1 = methodToFields.get(methodDeclaration1);
                Set<String> fieldsInMethod2 = methodToFields.get(methodDeclaration2);

                if (!Collections.disjoint(fieldsInMethod1, fieldsInMethod2)) {
                    methodsGraph.get(methodDeclaration1).add(methodDeclaration2);
                    methodsGraph.get(methodDeclaration2).add(methodDeclaration1);
                }
            }
        }
    }

    private static void getAccessedFieldsPerMethod(MethodDeclaration method, Set<String> fields, Map<String, Set<String>> methodToFields) {
        Set<String> fieldsAccessInMethod = new HashSet<>();
        method.findAll(FieldAccessExpr.class).forEach(field -> {
            String fieldName = field.getNameAsString();
            if (fields.contains(fieldName)) {
                fieldsAccessInMethod.add(fieldName);
            }
        });

        method.findAll(NameExpr.class).forEach(field -> ResolvableNode.resolve(field).forEach(resolvedField -> {
            if (!resolvedField.isField()) {
                return;
            }

            String fieldName = field.getNameAsString();
            if (fields.contains(fieldName)) {
                fieldsAccessInMethod.add(fieldName);
            }
        }));

        ResolvableNode.resolveOptional(method).ifPresent(methodSignature -> methodToFields.put(methodSignature.getSignature(), fieldsAccessInMethod));
    }

    @Override
    public Integer extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {
        Set<String> fields = new HashSet<>();

        classOrInterfaceDeclaration.getFields().forEach(field ->
                field.getVariables().forEach(variable -> fields.add(variable.getNameAsString()))
        );

        ResolvableNode.resolve(classOrInterfaceDeclaration)
                .flatMap(cls -> cls.getAllFields().stream())
                .filter(field -> !field.accessSpecifier().equals(AccessSpecifier.PRIVATE))
                .map(ResolvedDeclaration::getName)
                .forEach(fields::add);

        Map<String, Set<String>> methodToFields = new HashMap<>();
        classOrInterfaceDeclaration.getMethods().forEach(method -> getAccessedFieldsPerMethod(method, fields, methodToFields));

        Map<String, Set<String>> methodsGraph = new HashMap<>();

        for (String methodDeclaration : methodToFields.keySet()) {
            methodsGraph.put(methodDeclaration, new HashSet<>());
        }

        handleMethodFieldsRelations(methodsGraph, methodToFields);
        handleMethodCallsRelations(classOrInterfaceDeclaration, methodsGraph);

        int clusters = 0;
        Set<String> visitedMethods = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : methodsGraph.entrySet()) {
            if (!visitedMethods.contains(entry.getKey())) {
                doDfs(entry.getKey(), visitedMethods, methodsGraph);
                clusters++;
            }
        }


        return clusters;
    }

    @Override
    public String getPropertyName() {
        return "lcom4";
    }
}
