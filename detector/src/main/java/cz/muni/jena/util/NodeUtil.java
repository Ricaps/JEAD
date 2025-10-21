package cz.muni.jena.util;

import com.github.javaparser.ast.nodeTypes.NodeWithRange;

import java.util.Optional;

public class NodeUtil {

    private NodeUtil() {
        super();
    }


    public static Optional<Integer> getStartLineNumber(NodeWithRange<?> nodeWithRange) {
        return nodeWithRange.getRange().map(value -> value.begin.line);
    }
}
