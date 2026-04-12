package cz.muni.jena.inference.config.converter;

import cz.muni.jena.codeminer.extractor.CodeExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeExtractorConverterTest {

    @Test
    void convert_nullExtractorName_throwsIllegalArgumentException() {
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of());

        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Extractor name has to be defined!");
    }

    @Test
    void convert_unknownExtractorName_throwsIllegalArgumentException() {
        CodeExtractor<?> knownExtractor = mockExtractor("comments");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(knownExtractor));

        assertThatThrownBy(() -> converter.convert("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Extractor with name missing doesn't exist!");
    }

    @Test
    void convert_existingExtractorName_returnsMatchingExtractor() {
        CodeExtractor<?> commentsExtractor = mockExtractor("comments");
        CodeExtractor<?> metricsExtractor = mockExtractor("god-metrics");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(commentsExtractor, metricsExtractor));

        CodeExtractor<?> resolvedExtractor = converter.convert("god-metrics");

        assertThat(resolvedExtractor).isSameAs(metricsExtractor);
    }

    @Test
    void convert_duplicateIdentifiers_returnsFirstMatchingExtractor() {
        CodeExtractor<?> firstExtractor = mockExtractor("comments");
        CodeExtractor<?> secondExtractor = mockExtractor("comments");
        CodeExtractorConverter converter = new CodeExtractorConverter(List.of(firstExtractor, secondExtractor));

        CodeExtractor<?> resolvedExtractor = converter.convert("comments");

        assertThat(resolvedExtractor).isSameAs(firstExtractor);
    }

    private CodeExtractor<?> mockExtractor(String identifier) {
        CodeExtractor<?> extractor = mock(CodeExtractor.class);
        when(extractor.getIdentifier()).thenReturn(identifier);
        return extractor;
    }
}

