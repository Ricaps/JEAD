package cz.muni.jena.codeminer.extractor.god_di;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import cz.muni.jena.codeminer.extractor.god_di.metrics.MetricComputer;
import cz.muni.jena.codeminer.extractor.god_di.model.DIMetrics;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.configuration.di.Annotation;
import cz.muni.jena.configuration.di.DIConfiguration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.test_data.extractors.AnnotatedServiceFixture;
import cz.muni.jena.test_data.extractors.ServiceMarker;
import cz.muni.jena.utils.ParserTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GodDIMetricsExtractorTest {

    @Mock
    private Configuration configuration;

    @Mock
    private DIConfiguration diConfiguration;

    @Test
    void getIdentifier_returnsGodMetricsIdentifier() {
        GodDIMetricsExtractor extractor = new GodDIMetricsExtractor(List.of(), new ObjectMapper());

        assertThat(extractor.getIdentifier()).isEqualTo("god-metrics");
    }

    @Test
    void extract_withoutConfiguredAnnotation_returnsEmptyStream() {
        Configuration configuration = mockConfiguration(List.of(), List.of());
        GodDIMetricsExtractor extractor = new GodDIMetricsExtractor(List.of(), new ObjectMapper());

        List<DIMetrics> extracted = extractor.extract(parsedFixture(), configuration, new CommandSettingsHashMap()).toList();

        assertThat(extracted).isEmpty();
    }

    @Test
    void extract_withMatchingAnnotation_mapsMetricsAndEvaluatedNode() {
        MetricComputer<?> linesOfCode = mockMetricComputer("linesOfCode", 123);
        MetricComputer<?> methodsCount = mockMetricComputer("methodsCount", 2);

        GodDIMetricsExtractor extractor = new GodDIMetricsExtractor(List.of(linesOfCode, methodsCount), new ObjectMapper());
        Configuration configuration = mockConfiguration(List.of(new Annotation(ServiceMarker.class.getName())), List.of());

        List<DIMetrics> extracted = extractor.extract(parsedFixture(), configuration, new CommandSettingsHashMap()).toList();

        assertThat(extracted).hasSize(1);
        DIMetrics metrics = extracted.getFirst();
        assertThat(metrics.linesOfCode()).isEqualTo(123);
        assertThat(metrics.methodsCount()).isEqualTo(2);
        assertThat(metrics.code()).isNull();
        assertThat(metrics.evaluatedNode().fullyQualifiedName())
                .isEqualTo(AnnotatedServiceFixture.class.getName());
        assertThat(metrics.evaluatedNode().startLine()).isNotNull();
    }

    @Test
    void extract_withCodeSetting_includesSourceCode() {
        MetricComputer<?> linesOfCode = mockMetricComputer("linesOfCode", 1);

        GodDIMetricsExtractor extractor = new GodDIMetricsExtractor(List.of(linesOfCode), new ObjectMapper());
        Configuration configuration = mockConfiguration(List.of(new Annotation(ServiceMarker.class.getName())), List.of());
        CommandSettingsHashMap settings = new CommandSettingsHashMap();
        settings.put("code", "true");

        List<DIMetrics> extracted = extractor.extract(parsedFixture(), configuration, settings).toList();

        assertThat(extracted).hasSize(1);
        assertThat(extracted.getFirst().code()).contains("class AnnotatedServiceFixture");
    }

    @SuppressWarnings("unchecked")
    private MetricComputer<?> mockMetricComputer(String propertyName, Object metricValue) {
        MetricComputer<Object> metricComputer = mock(MetricComputer.class);
        when(metricComputer.getPropertyName()).thenReturn(propertyName);
        when(metricComputer.extractMetric(any(), any()))
                .thenReturn(metricValue);
        return metricComputer;
    }

    private Configuration mockConfiguration(List<Annotation> injectionAnnotations, List<Annotation> beanAnnotations) {
        when(configuration.diConfiguration()).thenReturn(diConfiguration);
        when(diConfiguration.injectionAnnotations()).thenReturn(injectionAnnotations);
        when(diConfiguration.beanAnnotations()).thenReturn(beanAnnotations);

        return configuration;
    }

    private ClassOrInterfaceDeclaration parsedFixture() {
        return ParserTest.getParsedClass(AnnotatedServiceFixture.class);
    }
}
