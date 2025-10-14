package cz.muni.jena.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueDao extends JpaRepository<Issue, Long>
{
    List<Issue> findAllByIssueType(IssueType issueType);
}
