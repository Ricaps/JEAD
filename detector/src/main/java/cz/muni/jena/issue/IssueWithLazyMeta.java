package cz.muni.jena.issue;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

/**
 * Wrapper around the {@link Issue}, where the metadata cannot be resolved directly in issue detector,
 * but has to be processed and saved later.
 * <br> <br>
 * One example of usage is Machine learning based issue detection, where issues are processed asynchronously.
 */
public record IssueWithLazyMeta(Issue issue, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
}
