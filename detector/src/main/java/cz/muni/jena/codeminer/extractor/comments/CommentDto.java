package cz.muni.jena.codeminer.extractor.comments;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record CommentDto(CommentType commentType, String text, @JsonIgnore Integer startLine, @JsonIgnore String fullyQualifiedName) {
    public static CommentDto ofJavadoc(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.JAVADOC, text, startLine, fullyQualifiedName);
    }

    public static CommentDto ofLine(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.LINE, text, startLine, fullyQualifiedName);
    }

    public static CommentDto ofBlock(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.BLOCK, text, startLine, fullyQualifiedName);
    }
}
