package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
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
public class CommentsCodeExtractor extends BaseCodeExtractor<Comment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentsCodeExtractor.class);
    private static final String COMMENTS_EXTRACTOR_IDENTIFIER = "comments";
    public static final String UNKNOWN_CLASS_NAME_MESSAGE = "Failed to obtain fully qualified name";

    CommentsCodeExtractor() {
        super(COMMENTS_EXTRACTOR_IDENTIFIER);
    }

    @Override
    public Stream<Comment> extract(ClassOrInterfaceDeclaration classOrInterface, Configuration configuration, CommandSettingsMap commandSettingsMap) {
        List<com.github.javaparser.ast.comments.Comment> comments = classOrInterface.getAllContainedComments();

        List<Comment> outputComments = processComments(comments, classOrInterface);

        LOGGER.info("Extracted {} comments from {}", outputComments.size(), classOrInterface.getNameAsString());

        return outputComments.stream();
    }

    private List<Comment> processComments(List<com.github.javaparser.ast.comments.Comment> comments, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        String fullyQualifiedName = getFullyQualifiedName(classOrInterfaceDeclaration);
        List<Comment> output = new LinkedList<>();
        for (com.github.javaparser.ast.comments.Comment comment : comments) {
            if (comment.isBlockComment()) {
                output.add(processBlockComment(comment.asBlockComment(), fullyQualifiedName));
            }
        }

        List<Comment> lineComments = processLineComments(comments, fullyQualifiedName);
        List<Comment> javadocComments = processJavadocComments(comments, fullyQualifiedName);

        output.addAll(lineComments);
        output.addAll(javadocComments);
        return output;
    }

    private List<Comment> processLineComments(List<com.github.javaparser.ast.comments.Comment> comments, String fullyQualifiedName) {
        return new LineCommentsWrapper(fullyQualifiedName, comments).processLineComment();
    }

    private List<Comment> processJavadocComments(List<com.github.javaparser.ast.comments.Comment> comments, String fullyQualifiedName) {
        return new JavadocCommentsWrapper(fullyQualifiedName, comments).parseJavadocComments();
    }

    private Comment processBlockComment(BlockComment comment, String fullyQualifiedName) {
        return Comment.ofBlock(CommentUtils.getTrimmedContent(comment), NodeUtil.getStartLineNumber(comment).orElse(null), fullyQualifiedName);
    }

    private static String getFullyQualifiedName(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getFullyQualifiedName().orElse(UNKNOWN_CLASS_NAME_MESSAGE);
    }

}
