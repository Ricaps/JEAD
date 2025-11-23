package cz.muni.jena.codeminer.extractor.comments.model;

import cz.muni.jena.inference.model.EvaluationModel;
import cz.muni.jena.codeminer.extractor.comments.CommentType;


public record Comment(CommentType commentType, String text, Integer startLine,
                      String fullyQualifiedName) implements EvaluationModel {

    public static Comment ofJavadoc(String text, Integer startLine, String fullyQualifiedName) {
        return new Comment(CommentType.JAVADOC, text, startLine, fullyQualifiedName);
    }

    public static Comment ofLine(String text, Integer startLine, String fullyQualifiedName) {
        return new Comment(CommentType.LINE, text, startLine, fullyQualifiedName);
    }

    public static Comment ofBlock(String text, Integer startLine, String fullyQualifiedName) {
        return new Comment(CommentType.BLOCK, text, startLine, fullyQualifiedName);
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
    public String getFullyQualifiedName() {
        return this.fullyQualifiedName;
    }

    @Override
    public Integer getStartLine() {
        return this.startLine;
    }
}
