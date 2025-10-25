package cz.muni.jena.codeminer.extractor.comments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.muni.jena.codeminer.EvaluatedNode;


public record CommentDto(CommentType commentType, String text, @JsonIgnore Integer startLine,
                         @JsonIgnore String fullyQualifiedName) implements EvaluatedNode {

    public static CommentDto ofJavadoc(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.JAVADOC, text, startLine, fullyQualifiedName);
    }

    public static CommentDto ofLine(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.LINE, text, startLine, fullyQualifiedName);
    }

    public static CommentDto ofBlock(String text, Integer startLine, String fullyQualifiedName) {
        return new CommentDto(CommentType.BLOCK, text, startLine, fullyQualifiedName);
    }

    @Override
    public String toString() {
        return "CommentDto[" +
                "commentType=" + commentType + ", " +
                "text=" + text + ", " +
                "startLine=" + startLine + ", " +
                "fullyQualifiedName=" + fullyQualifiedName + ']';
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getFullyQualifiedName() {
        return this.fullyQualifiedName;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer getStartLine() {
        return this.startLine;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getContent() {
        return String.format("%s: %s", commentType.toString(), text);
    }
}
