package cz.muni.jena.codeminer.extractor.god_di.metrics;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.test_data.god_di.*;
import cz.muni.jena.utils.ParserTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;


class LackOfCohesionOfMethodsMetricIT {

    private final LackOfCohesionOfMethodsMetric lcomExtractor = new LackOfCohesionOfMethodsMetric();

    @Test
    void test_oneCluster_returnsCorrectResult() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneCluster.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1);
    }

    @Test
    void test_threeClusters_returnsCorrectResult() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassThreeClusters.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(3);
    }

    @Test
    void test_twoClustersStaticMethodsAndFields_returnsCorrectResult() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassTwoClustersStaticMethods.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(2);
    }

    @Test
    void test_oneClusterStaticMethodsAndFields_returnsCorrectResult() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneClusterStaticMethods.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1);
    }

    @Test
    void test_oneClusterMethodsCalling_returnsCorrectResult() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassOneClusterMethodsCalling.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(1);
    }

    @Test
    void test_twoClustersMethodCallingToBaseClass_baseClassNotConsidered() {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = ParserTest.getParsedClass(TestClassTwoClustersMethodsCalling.class);

        int result = lcomExtractor.extractMetric(classOrInterfaceDeclaration, Mockito.mock());
        assertThat(result).isEqualTo(2);
    }
}