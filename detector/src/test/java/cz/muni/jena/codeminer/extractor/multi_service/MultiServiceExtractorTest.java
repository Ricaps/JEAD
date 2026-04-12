package cz.muni.jena.codeminer.extractor.multi_service;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.extractor.god_di.metrics.LackOfCohesionOfMethodsMetric;
import cz.muni.jena.codeminer.extractor.multi_service.model.MultiServiceMethods;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.Annotation;
import cz.muni.jena.configuration.service_layer.ServiceLayerConfiguration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.test_data.extractors.AnnotatedServiceFixture;
import cz.muni.jena.test_data.extractors.ServiceMarker;
import cz.muni.jena.utils.ParserTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiServiceExtractorTest {

    @Test
    void getIdentifier_returnsMultiServiceIdentifier() {
        MultiServiceExtractor extractor = new MultiServiceExtractor(mock(LackOfCohesionOfMethodsMetric.class));

        assertThat(extractor.getIdentifier()).isEqualTo("multi-service");
    }

    @Test
    void extract_withoutMatchingServiceAnnotation_returnsEmptyStream() {
        LackOfCohesionOfMethodsMetric lcomCalculator = mock(LackOfCohesionOfMethodsMetric.class);
        MultiServiceExtractor extractor = new MultiServiceExtractor(lcomCalculator);

        List<MultiServiceMethods> extracted = extractor.extract(parsedFixture(), mockConfiguration(Set.of()), new CommandSettingsHashMap())
                .toList();

        assertThat(extracted).isEmpty();
    }

    @Test
    void extract_withMatchingServiceAnnotation_returnsOnlyPublicAndPackageMethods() {
        LackOfCohesionOfMethodsMetric lcomCalculator = mock(LackOfCohesionOfMethodsMetric.class);
        when(lcomCalculator.extractMetric(any(), any())).thenReturn(7);

        MultiServiceExtractor extractor = new MultiServiceExtractor(lcomCalculator);
        Set<Annotation> serviceAnnotations = Set.of(new Annotation(ServiceMarker.class.getName()));

        List<MultiServiceMethods> extracted = extractor.extract(parsedFixture(), mockConfiguration(serviceAnnotations), new CommandSettingsHashMap())
                .toList();

        assertThat(extracted).hasSize(1);
        MultiServiceMethods result = extracted.getFirst();

        assertThat(result.lcom4()).isEqualTo(7);
        assertThat(result.fullyQualifiedName()).isEqualTo(AnnotatedServiceFixture.class.getName());
        assertThat(result.methods()).extracting(MultiServiceMethods.Method::name)
                .contains("publicOperation", "packageOperation")
                .doesNotContain("privateOperation");

        assertThat(result.methods())
                .extracting(MultiServiceMethods.Method::signature)
                .noneMatch(signature -> signature.contains("@"));
    }

    private Configuration mockConfiguration(Set<Annotation> serviceAnnotations) {
        Configuration configuration = mock(Configuration.class);
        ServiceLayerConfiguration serviceLayerConfiguration = mock(ServiceLayerConfiguration.class);

        when(configuration.serviceLayerConfiguration()).thenReturn(serviceLayerConfiguration);
        when(serviceLayerConfiguration.serviceAnnotations()).thenReturn(serviceAnnotations);

        return configuration;
    }

    private ClassOrInterfaceDeclaration parsedFixture() {
        return ParserTest.getParsedClass(AnnotatedServiceFixture.class);
    }
}





