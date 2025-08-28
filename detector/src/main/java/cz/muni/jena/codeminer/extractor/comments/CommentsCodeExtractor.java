package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.Range;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import cz.muni.jena.codeminer.extractor.BaseCodeExtractor;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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

        List<String> outputComments = processComments(comments);

        LOGGER.info("Extracted {} comments", comments.size());

        if (outputComments.isEmpty()) {
            return;
        }
        outputFormatter.add(new ArrayList<>(outputComments));
    }

    private List<String> processComments(List<Comment> comments) {
        List<String> output = new LinkedList<>();
        for (Comment comment : comments) {
            if (comment.isBlockComment()) {
                output.add(processBlockComment(comment.asBlockComment()));
            }
        }

        List<String> lineComments = processLineComments(comments);
        List<String> javadocComments = processJavadocComments(comments);

        output.addAll(lineComments);
        output.addAll(javadocComments);
        return output;
    }

    private List<String> processLineComments(List<Comment> comments) {
        return new LineCommentsWrapper(comments).processLineComment();
    }

    private List<String> processJavadocComments(List<Comment> comments) {
        return new JavadocCommentsWrapper(comments).parseJavadocComments();
    }

    private String processBlockComment(BlockComment comment) {
        return CommentUtils.getTrimmedContent(comment);
    }

    private Optional<Integer> getStartLineNumber(Comment lineComment) {
        Optional<Range> range = lineComment.getRange();
        return range.map(value -> value.begin.line);

    }
}
