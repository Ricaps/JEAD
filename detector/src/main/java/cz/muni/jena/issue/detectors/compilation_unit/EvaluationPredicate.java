package cz.muni.jena.issue.detectors.compilation_unit;

import cz.muni.jena.issue.IssueCategory;

/**
 * Predicate to test whether detector associated with provided IssueCategory should be run
 */
@FunctionalInterface
public interface EvaluationPredicate {

    boolean test(IssueCategory issueCategory);
}
