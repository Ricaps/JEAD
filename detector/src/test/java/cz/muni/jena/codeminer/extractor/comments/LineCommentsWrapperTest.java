package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LineCommentsWrapperTest {

    private static final String TEST_CLASS_PATH = "src/test/java/cz/muni/jena/test_data/LineCommentsParserTestClass.java";
    private static final String A_LOT_OF_SPACES_COMMENT = "a lot of spaces";
    private List<Comment> allContainedComments;
    private LineCommentsWrapper commentsWrapper;
    private static final Pattern SPACES_PATTERN = Pattern.compile(CommentUtils.SPACES_PATTERN);


    @BeforeEach
    void setup() throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(Path.of(TEST_CLASS_PATH));

        allContainedComments = compilationUnit.getAllContainedComments();
        allContainedComments.sort(CommentUtils.getLineSortComparator());
        commentsWrapper = new LineCommentsWrapper(allContainedComments);
    }

    @Test
    void processComments_commentsSquashed() {
        LinkedList<String> comments = commentsWrapper.processLineComment();

        String beanDeclaration = allContainedComments.get(3).getContent();
        String methodDeclaration = allContainedComments.get(4).getContent();
        String methodBlock = allContainedComments.get(5).getContent();
        String bracket = allContainedComments.get(6).getContent();

        assertThat(comments.getLast()).isEqualTo(getSquashedComment(beanDeclaration, methodDeclaration, methodBlock, bracket));
    }

    @Test
    void processComments_consecutiveSpaces_removed() {
        LinkedList<String> comments = commentsWrapper.processLineComment();

        Optional<String> comment = comments.stream().filter(comm -> comm.contains(A_LOT_OF_SPACES_COMMENT)).findFirst();
        assertThat(comment).isNotEmpty();
        assertThat(comment.get()).doesNotContainPattern(SPACES_PATTERN);
    }

    private String getSquashedComment(String ... comments) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String comment : comments) {
            stringBuilder.append(comment.trim()).append("\n");
        }

        return stringBuilder.toString();
    }



}