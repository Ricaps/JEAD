package cz.muni.jena.codeminer.extractor.comments;

import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import cz.muni.jena.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JavadocCommentsWrapper {

    private static final Set<JavadocBlockTag.Type> filteredTypes = Set.of(JavadocBlockTag.Type.AUTHOR);
    private final String fullyQualifiedName;
    private final List<JavadocComment> comments;

    public JavadocCommentsWrapper(String fullyQualifiedName, Collection<Comment> comments) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.comments = new ArrayList<>(comments)
                .stream()
                .filter(Comment::isJavadocComment)
                .map(Comment::asJavadocComment)
                .toList();
    }

    public List<CommentDto> parseJavadocComments() {
        return this.comments.stream()
                .flatMap(javadocComment -> {
                            Javadoc resolvedJavadoc = javadocComment.parse();
                            Integer startLine = NodeUtil.getStartLineNumber(javadocComment).orElse(null);
                            return Stream.concat(
                                    Stream.of(CommentDto.ofJavadoc(CommentUtils.getTrimmedContent(resolvedJavadoc.getDescription().toText()), startLine, fullyQualifiedName)),
                                    resolveBlockTags(javadocComment));
                        }
                )
                .toList();
    }

    private Stream<CommentDto> resolveBlockTags(JavadocComment javadocComment) {
        Javadoc javadoc = javadocComment.parse();
        String[] javadocTextSplit = javadoc.toText().split("\n");
        int javadocStartLine = NodeUtil.getStartLineNumber(javadocComment).orElse(0);

        return javadoc.getBlockTags()
                .stream()
                .filter(javadocBlockTag -> !filteredTypes.contains(javadocBlockTag.getType()))
                .map(JavadocBlockTag::toText)
                .map(CommentUtils::getTrimmedContent)
                .map(tagText -> {
                    Integer tagStartLine = CommentUtils.getRelativeJavadocLineNumber(javadocTextSplit, tagText).orElse(javadocStartLine);

                    return CommentDto.ofJavadoc(tagText, javadocStartLine + tagStartLine, this.fullyQualifiedName);
                });
    }

}
