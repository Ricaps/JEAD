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

import static org.assertj.core.api.Assertions.assertThat;

class LineCommentsWrapperTest {

    private static final String TEST_CLASS_PATH = "src/test/java/cz/muni/jena/test_data/LineCommentsParserTestClass.java";
    private List<Comment> allContainedComments;
    private LineCommentsWrapper commentsWrapper;


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

        assertThat(comments).hasSize(4);
        String beanDeclaration = allContainedComments.get(3).getContent();
        String methodDeclaration = allContainedComments.get(4).getContent();
        String methodBlock = allContainedComments.get(5).getContent();
        String bracket = allContainedComments.get(6).getContent();

        assertThat(comments.getLast()).isEqualTo(getSquashedComment(beanDeclaration, methodDeclaration, methodBlock, bracket));
    }

    private String getSquashedComment(String ... comments) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String comment : comments) {
            stringBuilder.append(comment.trim()).append("\n");
        }

        return stringBuilder.toString();
    }



}