package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.frontend.commands.commands.CommandSettingsMap;
import cz.muni.jena.util.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CommentsCodeExtractor extends BaseCodeExtractor<CommentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentsCodeExtractor.class);
    private static final String COMMENTS_EXTRACTOR_IDENTIFIER = "comments";
    public static final String UNKNOWN_CLASS_NAME_MESSAGE = "Failed to obtain fully qualified name";

    CommentsCodeExtractor() {
        super(COMMENTS_EXTRACTOR_IDENTIFIER);
    }

    @Override
    public Stream<CommentDto> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration, CommandSettingsMap commandSettingsMap) {
        List<Comment> comments = classOrInterface.getAllContainedComments();

        List<CommentDto> outputComments = processComments(comments, classOrInterface);

        LOGGER.info("Extracted {} comments from {}", outputComments.size(), classOrInterface.getNameAsString());

        return outputComments.stream();
    }

    private List<CommentDto> processComments(List<Comment> comments, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        String fullyQualifiedName = getFullyQualifiedName(classOrInterfaceDeclaration);
        List<CommentDto> output = new LinkedList<>();
        for (Comment comment : comments) {
            if (comment.isBlockComment()) {
                output.add(processBlockComment(comment.asBlockComment(), fullyQualifiedName));
            }
        }

        List<CommentDto> lineComments = processLineComments(comments, fullyQualifiedName);
        List<CommentDto> javadocComments = processJavadocComments(comments, fullyQualifiedName);

        output.addAll(lineComments);
        output.addAll(javadocComments);
        return output;
    }

    private List<CommentDto> processLineComments(List<Comment> comments, String fullyQualifiedName) {
        return new LineCommentsWrapper(fullyQualifiedName, comments).processLineComment();
    }

    private List<CommentDto> processJavadocComments(List<Comment> comments, String fullyQualifiedName) {
        return new JavadocCommentsWrapper(fullyQualifiedName, comments).parseJavadocComments();
    }

    private CommentDto processBlockComment(BlockComment comment, String fullyQualifiedName) {
        return CommentDto.ofBlock(CommentUtils.getTrimmedContent(comment), NodeUtil.getStartLineNumber(comment).orElse(null), fullyQualifiedName);
    }

    private static String getFullyQualifiedName(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getFullyQualifiedName().orElse(UNKNOWN_CLASS_NAME_MESSAGE);
    }

}
