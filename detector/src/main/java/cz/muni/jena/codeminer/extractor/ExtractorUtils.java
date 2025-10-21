package cz.muni.jena.codeminer.extractor;

import java.util.List;

public class ExtractorUtils {

    private ExtractorUtils() {
        super();
    }

    public static String getExtractorNames(List<CodeExtractor<?>> extractors) {
        return String.join(", ", extractors.stream().map(CodeExtractor::getIdentifier).toList());
    }
}
