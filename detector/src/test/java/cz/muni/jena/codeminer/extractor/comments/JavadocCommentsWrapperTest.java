package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.test_data.JavadocCommentsTestClass;
import cz.muni.jena.util.NodeUtil;
import cz.muni.jena.utils.ParserTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JavadocCommentsWrapperTest {

    public static final String FULLY_QUALIFIED_NAME = "Some qualified name";
    private static final Set<JavadocBlockTag.Type> filteredTags = Set.of(JavadocBlockTag.Type.AUTHOR, JavadocBlockTag.Type.UNKNOWN);
    private static final String DUMMY_METHOD = "dummyMethod";
    private static final String FORMAT_METHOD = "formatMethodComment";
    private static final Pattern SPACES_PATTERN = Pattern.compile(CommentUtils.SPACES_PATTERN);
    private JavadocCommentsWrapper dummyMethodCommentsWrapper;
    private JavadocCommentsWrapper formatMethodCommentsWrapper;
    private Javadoc dummyMethodJavadoc;
    private Integer dummyMethodStartLine;

    @BeforeEach
    void setup() {
        ClassOrInterfaceDeclaration compilationUnit = ParserTest.getParsedClass(JavadocCommentsTestClass.class);
        List<com.github.javaparser.ast.comments.Comment> allContainedComments = compilationUnit.getAllContainedComments();
        dummyMethodCommentsWrapper = new JavadocCommentsWrapper(FULLY_QUALIFIED_NAME, getJavadocOfMethod(DUMMY_METHOD, allContainedComments));
        formatMethodCommentsWrapper = new JavadocCommentsWrapper(FULLY_QUALIFIED_NAME, getJavadocOfMethod(FORMAT_METHOD, allContainedComments));

        dummyMethodJavadoc = allContainedComments.get(0).asJavadocComment().parse();
        dummyMethodStartLine = NodeUtil.getStartLineNumber(allContainedComments.get(0).asJavadocComment()).orElseThrow();
    }

    @Test
    void parseJavadocComments_commentType_allJavadoc() {
        List<Comment> dummyMethodComment = dummyMethodCommentsWrapper.parseJavadocComments();
        List<Comment> formatMethodComment = dummyMethodCommentsWrapper.parseJavadocComments();

        assertThat(dummyMethodComment).allMatch(commentDto -> commentDto.commentType().equals(CommentType.JAVADOC));
        assertThat(formatMethodComment).allMatch(commentDto -> commentDto.commentType().equals(CommentType.JAVADOC));
    }

    @Test
    void parseJavadocComments_description_isIncluded() {
        List<Comment> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        assertThat(comments.get(0).text()).isEqualTo(dummyMethodJavadoc.getDescription().toText());
    }

    @Test
    void parseJavadocComments_tags_authorExcluded() {
        List<Comment> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        String authorTagValue = dummyMethodJavadoc.getBlockTags().stream()
                .filter(tag -> tag.getType() == JavadocBlockTag.Type.AUTHOR)
                .findFirst()
                .orElseThrow()
                .toText();

        assertThat(comments).doesNotContain(Comment.ofJavadoc(authorTagValue, dummyMethodStartLine, FULLY_QUALIFIED_NAME));

    }

    @Test
    void parseJavadocComments_allExpectedTags_areIncluded() {
        List<Comment> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            var testedTag = dummyMethodJavadoc.getBlockTags().stream()
                    .filter(tag -> tag.getType().equals(tagType))
                    .findFirst();

            softAssertions.assertThat(testedTag).isNotEmpty();
            softAssertions.assertThat(comments).contains(Comment.ofJavadoc(testedTag.orElseThrow().toText(), getTagStartLine(testedTag.orElseThrow()), FULLY_QUALIFIED_NAME));

            var particularTagComment = comments.stream().filter(comment -> comment.text().contains(testedTag.get().toText())).findFirst().orElseThrow();
            softAssertions.assertThat(particularTagComment.getStartLine()).isEqualTo(getTagStartLine(testedTag.orElseThrow()));
        });

        softAssertions.assertAll();
    }

    @Test
    void parseJavadocComments_tags_haveExpectedValues() {
        List<Comment> comments = dummyMethodCommentsWrapper.parseJavadocComments();

        SoftAssertions softAssertions = new SoftAssertions();

        forEachIncludedTag(tagType -> {
            String tagName = new JavadocBlockTag(tagType, "").getTagName();
            String expectedValue = "@%s %s value".formatted(tagName, tagName);
            var testedTag = dummyMethodJavadoc.getBlockTags().stream()
                    .filter(tag -> tag.getType().equals(tagType))
                    .findFirst();

            softAssertions.assertThat(comments).contains(Comment.ofJavadoc(expectedValue, getTagStartLine(testedTag.orElseThrow()), FULLY_QUALIFIED_NAME));
        });

        softAssertions.assertAll();
    }

    @Test
    void parseJavadocComments_spaces_consecutiveSpacesRemoved() {
        List<Comment> comments = formatMethodCommentsWrapper.parseJavadocComments();

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).text()).doesNotContainPattern(SPACES_PATTERN);
    }

    private void forEachIncludedTag(Consumer<JavadocBlockTag.Type> consumer) {
        Stream.of(JavadocBlockTag.Type.values())
                .filter(tag -> !filteredTags.contains(tag))
                .forEach(consumer);
    }

    private List<com.github.javaparser.ast.comments.Comment> getJavadocOfMethod(String methodName, List<com.github.javaparser.ast.comments.Comment> comments) {
        return comments.stream()
                .filter(comment -> {
                    if (comment.getCommentedNode().orElseThrow() instanceof MethodDeclaration method) {
                        return method.getNameAsString().equals(methodName);
                    }

                    return false;
                })
                .toList();
    }


    private int getTagStartLine(JavadocBlockTag testedTag) {
        Optional<Integer> relativeJavadocLineNumber = CommentUtils.getRelativeJavadocLineNumber(dummyMethodJavadoc, testedTag.toText());

        return dummyMethodStartLine + relativeJavadocLineNumber.orElseThrow();
    }
}