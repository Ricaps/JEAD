package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

record LineCommentsWrapper(List<LineComment> lineComments) {

    public LineCommentsWrapper {
        lineComments = lineComments.stream()
                .filter(Comment::isLineComment)
                .map(Comment::asLineComment)
                .sorted(Comparator.comparing(comment -> {
                    if (comment.getRange().isPresent()) {
                        return comment.getRange().get().begin.line;
                    }

                    return -1;
                }))
                .toList();
    }

    /**
     * Processes line comments.
     * If there are comments on consecutive lines, it squashes them
     * @return list of strings. Each element contains one comment.
     */
    public List<String> processLineComment() {
        int lastLine = -1;
        LinkedList<String> result = new LinkedList<>();
        StringBuilder toBeSquashed = new StringBuilder();

        for (LineComment lineComment : lineComments) {
            Optional<Integer> optionalLineNumber = getStartLineNumber(lineComment);
            Optional<LineComment> previousLineComment = getPreviousLineComment(lineComment);
            if (optionalLineNumber.isPresent() && optionalLineNumber.get() - 1 == lastLine && !isLineWithCommentAndCode(previousLineComment.orElse(null))) {

                if (toBeSquashed.isEmpty()) {
                    toBeSquashed.append(result.getLast()).append("\n");
                    result.removeLast();
                }
                toBeSquashed.append(CommentUtils.getTrimmedContent(lineComment)).append("\n");
            } else {
                 if (!toBeSquashed.isEmpty()) {
                    result.add(toBeSquashed.toString());
                }
                result.add(CommentUtils.getTrimmedContent(lineComment));
                toBeSquashed.setLength(0);
            }

            if (optionalLineNumber.isPresent()) {
                lastLine = optionalLineNumber.get();
            }
        }

        if (!toBeSquashed.isEmpty()) {
            result.add(toBeSquashed.toString());
        }

        return result;
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
        Optional<Integer> optionalCommentLineNumber = getStartLineNumber(lineComment);

        if (optionalNodeLineNumber.isEmpty() || optionalCommentLineNumber.isEmpty()) {
            return false;
        }

        return optionalNodeLineNumber.get().equals(optionalCommentLineNumber.get());
    }

    private Optional<Integer> getStartLineNumber(Node lineComment) {
        Optional<Range> range = lineComment.getRange();
        return range.map(value -> value.begin.line);
    }

    private Optional<Integer> getEndLineNumber(Node lineComment) {
        Optional<Range> range = lineComment.getRange();
        return range.map(value -> value.end.line);
    }
}
