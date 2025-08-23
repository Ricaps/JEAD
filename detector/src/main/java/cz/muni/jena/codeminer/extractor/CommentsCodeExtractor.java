package cz.muni.jena.codeminer.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.Comment;
import cz.muni.jena.codeminer.outputformatter.OutputFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<String> comments = classOrInterface
                .findAll(Comment.class)
                .stream()
                .map(Comment::toString)
                .filter(comment -> !comment.isBlank())
                .toList();

        LOGGER.info("Extracted {} comments", comments.size());

        if (comments.isEmpty()) {
            return;
        }
        outputFormatter.add(new ArrayList<>(comments));
    }
}
