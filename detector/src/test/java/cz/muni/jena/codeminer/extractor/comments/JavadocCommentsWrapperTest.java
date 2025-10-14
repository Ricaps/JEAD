package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import cz.muni.jena.util.NodeUtil;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JavadocCommentsWrapperTest {

    private static final Path CLASS_PATH = Path.of("src/test/java/cz/muni/jena/test_data/JavadocCommentsTestClass.java");
    private static final Set<JavadocBlockTag.Type> filteredTags = Set.of(JavadocBlockTag.Type.AUTHOR, JavadocBlockTag.Type.UNKNOWN);
    private static final String DUMMY_METHOD = "dummyMethod";
    private static final String FORMAT_METHOD = "formatMethodComment";
    private static final Pattern SPACES_PATTERN = Pattern.compile(CommentUtils.SPACES_PATTERN);
    public static final String FULLY_QUALIFIED_NAME = "Some qualified name";
    private JavadocCommentsWrapper dummyMethodCommentsWrapper;
    private JavadocCommentsWrapper formatMethodCommentsWrapper;
    private Javadoc dummyMethodJavadoc;
    private Integer dummyMethodStartLine;

    @BeforeEach
    void setup() throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(CLASS_PATH);
        List<Comment> allContainedComments = compilationUnit.getAllContainedComments();
        dummyMethodCommentsWrapper = new JavadocCommentsWrapper(FULLY_QUALIFIED_NAME, getJavadocOfMethod(DUMMY_METHOD, allContainedComments));
        formatMethodCommentsWrapper = new JavadocCommentsWrapper(FULLY_QUALIFIED_NAME, getJavadocOfMethod(FORMAT_METHOD, allContainedComments));

        dummyMethodJavadoc = allContainedComments.get(0).asJavadocComment().parse();
        dummyMethodStartLine = NodeUtil.getStartLineNumber(allContainedComments.get(0).asJavadocComment()).orElseThrow();
    }

    @Test
    void parseJavadocComments_commentType_allJavadoc() {
        List<CommentDto> dummyMethodComment = dummyMethodCommentsWrapper.parseJavadocComments();
        List<CommentDto> formatMethodComment = dummyMethodCommentsWrapper.parseJavadocComments();

        assertThat(dummyMethodComment).allMatch(commentDto -> commentDto.commentType().equals(CommentType.JAVADOC));
        assertThat(formatMethodComment).allMatch(commentDto -> commentDto.commentType().equals(CommentType.JAVADOC));
    }

    @Test
    void parseJavadocComments_description_isIncluded() {
        List<CommentDto> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        assertThat(comments.get(0).text()).isEqualTo(dummyMethodJavadoc.getDescription().toText());
    }

    @Test
    void parseJavadocComments_tags_authorExcluded() {
        List<CommentDto> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        String authorTagValue = dummyMethodJavadoc.getBlockTags().stream()
                .filter(tag -> tag.getType() == JavadocBlockTag.Type.AUTHOR)
                .findFirst()
                .orElseThrow()
                .toText();

        assertThat(comments).doesNotContain(CommentDto.ofJavadoc(authorTagValue, dummyMethodStartLine, FULLY_QUALIFIED_NAME));

    }

    @Test
    void parseJavadocComments_allExpectedTags_areIncluded() {
        List<CommentDto> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            var testedTag = dummyMethodJavadoc.getBlockTags().stream()
                    .filter(tag -> tag.getType().equals(tagType))
                    .findFirst();

            softAssertions.assertThat(testedTag).isNotEmpty();
            Optional<Integer> relativeJavadocLineNumber = CommentUtils.getRelativeJavadocLineNumber(dummyMethodJavadoc, testedTag.orElseThrow().toText());
            softAssertions.assertThat(comments).contains(CommentDto.ofJavadoc(testedTag.orElseThrow().toText(), dummyMethodStartLine + relativeJavadocLineNumber.orElseThrow(), FULLY_QUALIFIED_NAME));

            var particularTagComment = comments.stream().filter(comment -> comment.getContent().contains(testedTag.get().toText())).findFirst().orElseThrow();
            softAssertions.assertThat(particularTagComment.getStartLine()).isEqualTo(dummyMethodStartLine + relativeJavadocLineNumber.orElseThrow());
        });

        softAssertions.assertAll();
    }

    @Test
    void parseJavadocComments_tags_haveExpectedValues() {
        List<CommentDto> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            String tagName = new JavadocBlockTag(tagType, "").getTagName();
            String expectedValue = "@%s %s value".formatted(tagName, tagName);

            softAssertions.assertThat(comments).contains(CommentDto.ofJavadoc(expectedValue, dummyMethodStartLine, FULLY_QUALIFIED_NAME));
        });

        softAssertions.assertAll();
    }

    @Test
    void parseJavadocComments_spaces_consecutiveSpacesRemoved() {
        List<CommentDto> comments = formatMethodCommentsWrapper.parseJavadocComments();

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).text()).doesNotContainPattern(SPACES_PATTERN);
    }

    private void forEachIncludedTag(Consumer<JavadocBlockTag.Type> consumer) {
        Stream.of(JavadocBlockTag.Type.values())
                .filter(tag -> !filteredTags.contains(tag))
                .forEach(consumer);
    }

    private List<Comment> getJavadocOfMethod(String methodName, List<Comment> comments) {
        return comments.stream()
                .filter(comment -> {
                    if (comment.getCommentedNode().orElseThrow() instanceof MethodDeclaration method) {
                        return method.getNameAsString().equals(methodName);
                    }

                    return false;
                })
                .toList();
    }

}