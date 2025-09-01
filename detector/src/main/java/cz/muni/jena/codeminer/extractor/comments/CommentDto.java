package cz.muni.jena.codeminer.extractor.comments;

public record CommentDto(CommentType commentType, String text) {
    public static CommentDto ofJavadoc(String text) {
        return new CommentDto(CommentType.JAVADOC, text);
    }

    public static CommentDto ofLine(String text) {
        return new CommentDto(CommentType.LINE, text);
    }

    public static CommentDto ofBlock(String text) {
        return new CommentDto(CommentType.BLOCK, text);
    }
}
