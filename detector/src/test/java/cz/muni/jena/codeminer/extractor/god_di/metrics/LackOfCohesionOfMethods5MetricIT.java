package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.test_data.god_di.lcom5.*;
import cz.muni.jena.utils.ParserTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;


class LackOfCohesionOfMethods5MetricIT {

    private final LackOfCohesionOfMethodsMetric5 lcomExtractor = new LackOfCohesionOfMethodsMetric5();

    @Test
    void test_noField_returns0() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassNoField.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void test_oneFieldNoMethod_returns0() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneFieldNoMethod.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void test_oneMethodOneFieldUsedByMethod_returns0() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneMethodAndField.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void test_oneMethodOneFieldNotUsedByMethod_returns1() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneMethodAndFieldNotUsed.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    void test_multipleMethodUsingOneField_returns0() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassMultipleMethodsOneField.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void test_multipleMethodUsingMultipleFieldsOneNotUsed_returnsMoreThanOne() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassMultipleMethodsMultipleFields.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1.13);
    }

    @Test
    void multipleMethodUsingMultipleFields_oneMethodUsesOneField_returns1() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassEachMethodUsesOneField.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    void multipleMethodUsingMultipleFields_someMethodsUsesMoreFields_returnsBelowOne() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassSomeMethodsUsesMoreFields.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.83);
    }

    @Test
    void multipleMethodUsingMultipleFields_oneMethodUsesFieldMultipleTimes_multipleUsesCountsAsOneUseReturnsBelowOne() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassMethodUsesFieldMultipleTimes.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.83);
    }

    @Test
    void multipleMethodUsingMultipleFields_allMethodsUsesAllFields_returns0() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassAllMethodsUsesAllFields.class);

        double result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(0.0);
    }

}