package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;

public class CommentUtils {

    private CommentUtils() {
        super();
    }

    public static String getTrimmedContent(Comment comment) {
        return comment.getContent().trim();
    }
}
