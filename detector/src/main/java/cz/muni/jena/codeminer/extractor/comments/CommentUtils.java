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
        String[] javadocTextSplit = javadoc.toText().split("\n");

        return getRelativeJavadocLineNumber(javadocTextSplit, javadocTagText);
    }

    static Optional<Integer> getRelativeJavadocLineNumber(String[] javadocTextSplit, String javadocTagText) {
        for (int i = 0; i < javadocTagText.length(); i++) {
            String line = javadocTextSplit[i];

            if (line.contains(javadocTagText)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }
}
