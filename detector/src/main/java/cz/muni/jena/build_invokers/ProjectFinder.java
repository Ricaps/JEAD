package cz.muni.jena.build_invokers;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface ProjectFinder {

    Set<Path> process(List<Path> paths);
    String getFileSuffix();
}
