package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.LineComment;
import cz.muni.jena.codeminer.extractor.comments.model.Comment;
import cz.muni.jena.util.NodeUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class LineCommentsWrapper {

    private final String fullyQualifiedName;
    private final List<LineComment> lineComments;

    public LineCommentsWrapper(String fullyQualifiedName, List<com.github.javaparser.ast.comments.Comment> comments) {
        this.fullyQualifiedName = fullyQualifiedName;
        lineComments = comments.stream()
                .filter(com.github.javaparser.ast.comments.Comment::isLineComment)
                .map(com.github.javaparser.ast.comments.Comment::asLineComment)
                .sorted(CommentUtils.getLineSortComparator())
                .toList();
    }

    /**
     * Processes line comments.
     * If there are comments on consecutive lines, it squashes them
     *
     * @return list of strings. Each element contains one comment.
     */
    public LinkedList<Comment> processLineComment() {
        int lastLine = -1;
        LinkedList<Comment> result = new LinkedList<>();
        ToBeSquashedComment toBeSquashed = null;

        for (LineComment lineComment : lineComments) {
            Optional<Integer> optionalLineNumber = NodeUtil.getStartLineNumber(lineComment);
            if (canBeSquashed(lineComment, lastLine)) {

                if (toBeSquashed == null) {
                    toBeSquashed = new ToBeSquashedComment(optionalLineNumber.orElse(null));
                    toBeSquashed.comment.append(result.getLast().text()).append("\n");
                    result.removeLast();
                }
                toBeSquashed.comment.append(CommentUtils.getTrimmedContent(lineComment)).append("\n");
            } else {
                if (toBeSquashed != null) {
                    result.add(Comment.ofLine(toBeSquashed.comment.toString(), toBeSquashed.startLine, fullyQualifiedName));
                    toBeSquashed.comment.setLength(0);
                }
                result.add(Comment.ofLine(CommentUtils.getTrimmedContent(lineComment), optionalLineNumber.orElse(null), fullyQualifiedName));
            }

            if (optionalLineNumber.isPresent()) {
                lastLine = optionalLineNumber.get();
            }
        }

        if (toBeSquashed != null) {
            result.add(Comment.ofLine(toBeSquashed.comment.toString(), toBeSquashed.startLine, fullyQualifiedName));
        }

        return result;
    }

    private boolean canBeSquashed(LineComment lineComment, int lastLine) {
        Optional<Integer> optionalLineNumber = NodeUtil.getStartLineNumber(lineComment);
        Optional<LineComment> previousLineComment = getPreviousLineComment(lineComment);

        if (optionalLineNumber.isEmpty()) {
            return false;
        }

        if (optionalLineNumber.get() - 1 != lastLine) {
            return false;
        }

        if (isLineWithCommentAndCode(previousLineComment.orElse(null))) {
            // If previous line is comment with code, cannot be squashed
            return false;
        }

        // If previous line is comment with code and current line is just comment, also cannot be squashed
        return !isLineWithCommentAndCode(lineComment);
    }

    private Optional<LineComment> getPreviousLineComment(LineComment lineComment) {
        int index = lineComments.indexOf(lineComment) - 1;

        if (index < 0) {
            return Optional.empty();
        }

        return Optional.of(lineComments.get(index));
    }

    private boolean isLineWithCommentAndCode(LineComment lineComment) {
        if (lineComment == null) {
            return false;
        }

        Optional<Node> commentedNode = lineComment.getCommentedNode();
        if (commentedNode.isEmpty()) {
            return false;
        }

        Optional<Integer> optionalNodeLineNumber = getEndLineNumber(commentedNode.get());
        Optional<Integer> optionalCommentLineNumber = NodeUtil.getStartLineNumber(lineComment);

        if (optionalNodeLineNumber.isEmpty() || optionalCommentLineNumber.isEmpty()) {
            return false;
        }

        return optionalNodeLineNumber.get().equals(optionalCommentLineNumber.get());
    }

    private Optional<Integer> getEndLineNumber(Node lineComment) {
        Optional<Range> range = lineComment.getRange();
        return range.map(value -> value.end.line);
    }

    private record ToBeSquashedComment(Integer startLine, StringBuilder comment) {
        ToBeSquashedComment(Integer startLine) {
            this(startLine, new StringBuilder());
        }
    }
}
