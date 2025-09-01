package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class CommentsCodeExtractor extends BaseCodeExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentsCodeExtractor.class);
    private static final String COMMENTS_EXTRACTOR_IDENTIFIER = "comments";

    CommentsCodeExtractor() {
        super(COMMENTS_EXTRACTOR_IDENTIFIER);
    }

    @Override
    public void extract(ClassOrInterfaceDeclaration classOrInterface, OutputFormatter outputFormatter) {
        List<Comment> comments = classOrInterface.getAllContainedComments();

        List<CommentDto> outputComments = processComments(comments);

        LOGGER.info("Extracted {} comments from {}", comments.size(), classOrInterface.getNameAsString());

        if (outputComments.isEmpty()) {
            return;
        }
        outputFormatter.add(new ArrayList<>(outputComments));
    }

    private List<CommentDto> processComments(List<Comment> comments) {
        List<CommentDto> output = new LinkedList<>();
        for (Comment comment : comments) {
            if (comment.isBlockComment()) {
                output.add(processBlockComment(comment.asBlockComment()));
            }
        }

        List<CommentDto> lineComments = processLineComments(comments);
        List<CommentDto> javadocComments = processJavadocComments(comments);

        output.addAll(lineComments);
        output.addAll(javadocComments);
        return output;
    }

    private List<CommentDto> processLineComments(List<Comment> comments) {
        return new LineCommentsWrapper(comments).processLineComment();
    }

    private List<CommentDto> processJavadocComments(List<Comment> comments) {
        return new JavadocCommentsWrapper(comments).parseJavadocComments();
    }

    private CommentDto processBlockComment(BlockComment comment) {
        return CommentDto.ofBlock(CommentUtils.getTrimmedContent(comment));
    }

}
