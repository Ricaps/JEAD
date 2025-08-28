package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;

import java.util.Comparator;

public class CommentUtils {

    private CommentUtils() {
        super();
    }

    public static String getTrimmedContent(Comment comment) {
        return comment.getContent().trim();
    }

    public static Comparator<Comment> getLineSortComparator() {
        return Comparator.comparing(comment -> {
            if (comment.getRange().isPresent()) {
                return comment.getRange().get().begin.line;
            }

            return -1;
        });
    }
}
