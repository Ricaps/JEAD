package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsHashMap;
import cz.muni.jena.test_data.JavadocCommentsTestClass;
import cz.muni.jena.test_data.LineCommentsParserTestClass;
import cz.muni.jena.utils.ParserTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommentsCodeExtractorTest {

    private final CommentsCodeExtractor extractor = new CommentsCodeExtractor();

    @Mock
    private Configuration configuration;

    @Test
    void getIdentifier_returnsCommentsIdentifier() {
        assertThat(extractor.getIdentifier()).isEqualTo("comments");
    }

    @Test
    void extract_lineComments_returnsLineCommentRecordsWithParsedFqn() {
        ClassOrInterfaceDeclaration parsedClass = ParserTest.getParsedClass(LineCommentsParserTestClass.class);

        List<Comment> extractedComments = extractor.extract(parsedClass, configuration, new CommandSettingsHashMap())
                .toList();

        assertThat(extractedComments).isNotEmpty();
        assertThat(extractedComments).anyMatch(comment -> comment.commentType() == CommentType.LINE);
        assertThat(extractedComments)
                .allMatch(comment -> comment.fullyQualifiedName().equals("cz.muni.jena.test_data.LineCommentsParserTestClass"));
    }

    @Test
    void extract_javadocs_returnsJavadocCommentRecords() {
        ClassOrInterfaceDeclaration parsedClass = ParserTest.getParsedClass(JavadocCommentsTestClass.class);

        List<Comment> extractedComments = extractor.extract(parsedClass, configuration, new CommandSettingsHashMap())
                .toList();

        assertThat(extractedComments).anyMatch(comment -> comment.commentType() == CommentType.JAVADOC);
    }

    @Test
    void extract_withoutResolvedFqn_usesFallbackClassNameMessage() {
        ClassOrInterfaceDeclaration detachedClass = new ClassOrInterfaceDeclaration();
        detachedClass.setName("DetachedClass");
        detachedClass.addOrphanComment(new BlockComment("  detached block comment  "));

        List<Comment> extractedComments = extractor.extract(detachedClass, configuration, new CommandSettingsHashMap())
                .toList();

        assertThat(extractedComments).hasSize(1);
        assertThat(extractedComments.getFirst().fullyQualifiedName())
                .isEqualTo(CommentsCodeExtractor.UNKNOWN_CLASS_NAME_MESSAGE);
    }
}
