package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JavadocCommentsWrapper {
    private final List<JavadocComment> comments;
    private static final Set<JavadocBlockTag.Type> filteredTypes = Set.of(JavadocBlockTag.Type.AUTHOR);

    public JavadocCommentsWrapper(Collection<Comment> comments) {
        this.comments = new ArrayList<>(comments)
                .stream()
                .filter(Comment::isJavadocComment)
                .map(Comment::asJavadocComment)
                .toList();
    }

    public List<CommentDto> parseJavadocComments() {
        return this.comments.stream()
                .map(JavadocComment::parse)
                .flatMap(javadoc -> Stream.concat(
                        Stream.of(javadoc.getDescription().toText()),
                        resolveBlockTags(javadoc))
                )
                .map(CommentUtils::getTrimmedContent)
                .map(CommentDto::ofJavadoc)
                .toList();
    }

    private Stream<String> resolveBlockTags(Javadoc javadoc) {
        return javadoc.getBlockTags()
                .stream()
                .filter(javadocBlockTag -> !filteredTypes.contains(javadocBlockTag.getType()))
                .map(JavadocBlockTag::toText);
    }
}
