package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;

import java.util.Comparator;

public class CommentUtils {

    public static final String SPACES_PATTERN = " {2,}";

    private CommentUtils() {
        super();
    }

    public static String getTrimmedContent(Comment comment) {
        return getTrimmedContent(comment.getContent());
    }

    public static String getTrimmedContent(String string) {
        return string.trim().replaceAll(SPACES_PATTERN, " ");
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
