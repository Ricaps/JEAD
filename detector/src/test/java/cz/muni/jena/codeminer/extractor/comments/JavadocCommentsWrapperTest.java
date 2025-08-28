package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JavadocCommentsWrapperTest {

    private static final Path CLASS_PATH = Path.of("src/test/java/cz/muni/jena/test_data/JavadocCommentsTestClass.java");
    private static final Set<JavadocBlockTag.Type> filteredTags = Set.of(JavadocBlockTag.Type.AUTHOR, JavadocBlockTag.Type.UNKNOWN);
    private JavadocCommentsWrapper commentsWrapper;
    private Javadoc parsedJavadoc;

    @BeforeEach
    void setup() throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(CLASS_PATH);
        List<Comment> allContainedComments = compilationUnit.getAllContainedComments();
        commentsWrapper = new JavadocCommentsWrapper(allContainedComments);

        assertThat(allContainedComments).hasSize(1);
        assertThat(allContainedComments.get(0).isJavadocComment()).isTrue();
        parsedJavadoc = allContainedComments.get(0).asJavadocComment().parse();
    }

    @Test
    void parseJavadocComments_description_isIncluded() {
        List<String> comments = commentsWrapper.parseJavadocComments();

        assertThat(comments).first().isEqualTo(parsedJavadoc.getDescription().toText());
    }

    @Test
    void parseJavadocComments_tags_authorExcluded() {
        List<String> comments = commentsWrapper.parseJavadocComments();

        String authorTagValue = parsedJavadoc.getBlockTags().stream()
                .filter(tag -> tag.getType() == JavadocBlockTag.Type.AUTHOR)
                .findFirst()
                .orElseThrow()
                .toText();

        assertThat(comments).doesNotContain(authorTagValue);

    }

    @Test
    void parseJavadocComments_allExpectedTags_areIncluded() {
        List<String> comments = commentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            var testedTag = parsedJavadoc.getBlockTags().stream()
                    .filter(tag -> tag.getType().equals(tagType))
                    .findFirst();

            softAssertions.assertThat(testedTag).isNotEmpty();
            softAssertions.assertThat(comments).contains(testedTag.orElseThrow().toText());
        });

        softAssertions.assertAll();
    }

    @Test
    void parseJavadocComments_tags_haveExpectedValues() {
        List<String> comments = commentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            String tagName = new JavadocBlockTag(tagType, "").getTagName();
            String expectedValue = "@%s %s value".formatted(tagName, tagName);

            softAssertions.assertThat(comments).contains(expectedValue);
        });

        softAssertions.assertAll();
    }

    private void forEachIncludedTag(Consumer<JavadocBlockTag.Type> consumer) {
        Stream.of(JavadocBlockTag.Type.values())
                .filter(tag -> !filteredTags.contains(tag))
                .forEach(consumer);
    }

}