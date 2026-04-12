package cz.muni.jena.inference.config.converter;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeExtractorConverterTest {

    @Mock
    private CodeExtractor<?> firstExtractor;

    @Mock
    private CodeExtractor<?> secondExtractor;

    @Mock
    private CodeExtractor<?> metricsExtractor;

    @Test
    void convert_nullExtractorName_throwsIllegalArgumentException() {
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of());

        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Extractor name has to be defined!");
    }

    @Test
    void convert_unknownExtractorName_throwsIllegalArgumentException() {
        when(firstExtractor.getIdentifier()).thenReturn("comments");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(firstExtractor));

        assertThatThrownBy(() -> converter.convert("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Extractor with name missing doesn't exist!");
    }

    @Test
    void convert_existingExtractorName_returnsMatchingExtractor() {
        when(firstExtractor.getIdentifier()).thenReturn("comments");
        when(metricsExtractor.getIdentifier()).thenReturn("god-metrics");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(firstExtractor, metricsExtractor));

        CodeExtractor<?> resolvedExtractor = converter.convert("god-metrics");

        assertThat(resolvedExtractor).isSameAs(metricsExtractor);
    }

    @Test
    void convert_duplicateIdentifiers_returnsFirstMatchingExtractor() {
        when(firstExtractor.getIdentifier()).thenReturn("comments");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(firstExtractor, secondExtractor));

        CodeExtractor<?> resolvedExtractor = converter.convert("comments");

        assertThat(resolvedExtractor).isSameAs(firstExtractor);
    }
}
