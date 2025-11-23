package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.language.elements.ResolvableNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class LackOfCohesionOfMethodsMetric5 implements MetricComputer<Double> {

    private static int getAccessedFieldsPerMethod(MethodDeclaration method, Set<String> fields) {
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

        return fieldsAccessInMethod.size();

    }

    private static Set<String> getFields(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        Set<String> fields = new HashSet<>();

        classOrInterfaceDeclaration.getFields().forEach(field ->
                field.getVariables().forEach(variable -> fields.add(variable.getNameAsString()))
        );

        return fields;
    }

    @Override
    public Double extractMetric(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Configuration configuration) {

        Set<String> fields = getFields(classOrInterfaceDeclaration);

        double numberOfFields = fields.size(); // l
        if (numberOfFields == 0) {
            return 0.0;
        }
        double numberOfMethods = getNumberOfMethods(classOrInterfaceDeclaration); // k
        double numberOfAccesses = classOrInterfaceDeclaration.getMethods().stream().mapToInt(method -> getAccessedFieldsPerMethod(method, fields)).sum(); // a

        // Formula: LCOM5 = (a - k*l)/(l-k*l)
        double result = (numberOfAccesses - (numberOfMethods * numberOfFields)) / (numberOfFields - (numberOfMethods * numberOfFields));

        if (Double.isInfinite(result) && numberOfFields == 1.0) {
            return 1.0;
        }

        return Math.round(result * 100) / 100.0;
    }

    private int getNumberOfMethods(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.findAll(MethodDeclaration.class).stream().filter(method -> !method.isStatic()).toList().size();
    }

    @Override
    public String getPropertyName() {
        return "lcom5";
    }
}
