package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.javadoc.Javadoc;

import java.util.Comparator;
import java.util.Optional;

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

    static Optional<Integer> getRelativeJavadocLineNumber(Javadoc javadoc, String javadocTagText) {
        String[] javadocLinesSplit = javadoc.toText().split("\n");

        return getRelativeJavadocLineNumber(javadocLinesSplit, javadocTagText);
    }

    static Optional<Integer> getRelativeJavadocLineNumber(String[] javadocLinesSplit, String javadocTagText) {
        for (int i = 0; i < javadocLinesSplit.length; i++) {
            String line = javadocLinesSplit[i];

            if (line.contains(javadocTagText)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }
}
