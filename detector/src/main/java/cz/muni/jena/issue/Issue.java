package cz.muni.jena.issue;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "Issue",
        uniqueConstraints = {@UniqueConstraint(columnNames = {
                "issue_type",
                "line_number",
                "fully_qualified_name",
                "project_label",
                "analysis_type"
        },
        name = "IssueUniqueConstraint")}
)
public final class Issue
{
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type")
    private IssueType issueType;

    @Column(name = "line_number")
    private String lineNumber;

    @Column(name = "fully_qualified_name")
    private String fullyQualifiedName;

    @Column(name = "project_label")
    private String projectLabel;

    @Column(name = "analysis_type")
    @Enumerated(EnumType.STRING)
    private AnalysisType analysisType = AnalysisType.STATIC;

    @ManyToOne
    private IssueClass issueClass;

    @ManyToOne
    private IssueMethod method;

    public Issue()
    {}

    public Issue(IssueType issueType, Integer lineNumber, String fullyQualifiedName)
    {
        this.issueType = issueType;
        this.lineNumber = Optional.ofNullable(lineNumber).map(String::valueOf).orElse("None");
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public Issue(IssueType issueType, Integer lineNumber, String fullyQualifiedName, AnalysisType analysisType)
    {
        this(issueType, lineNumber, fullyQualifiedName);
        this.analysisType = analysisType;
    }

    public static Issue fromNodeWithRange(
            NodeWithRange<?> nodeWithRange,
            IssueType type,
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration
    )
    {
        return new Issue(
                type,
                Optional.ofNullable(nodeWithRange)
                        .flatMap(NodeWithRange::getBegin)
                        .map(beginning -> beginning.line)
                        .orElse(null),
                classOrInterfaceDeclaration.getFullyQualifiedName().orElse("")
        );
    }

    public static Issue fromClass(
            IssueType type,
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration
    )
    {
        return new Issue(
                type,
                null,
                classOrInterfaceDeclaration.getFullyQualifiedName().orElse("")
        );
    }

    @Override
    public String toString()
    {
        return "Issue{" +
                "issueType=" + issueType +
                ", lineNumber=" + Optional.ofNullable(lineNumber).map(Objects::toString).orElse("Not present") +
                ", fullyQualifiedName='" + fullyQualifiedName + '\'' +
                '}';
    }

    public String[] toTableRow()
    {
        return new String[] {
                issueType.toString() + " ",
                Optional.ofNullable(lineNumber).map(Objects::toString).orElse("None") + " ",
                fullyQualifiedName + " ",
                Optional.ofNullable(method).map(IssueMethod::getName).orElse("None") + " ",
                analysisType.name() + " "
        };
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public IssueType getIssueType()
    {
        return issueType;
    }

    public void setIssueType(IssueType issueType)
    {
        this.issueType = issueType;
    }

    public String getLineNumber()
    {
        return lineNumber;
    }

    public Optional<Integer> getLineNumberAsInt()
    {
        try
        {
            return Optional.of(Integer.parseInt(lineNumber));
        } catch (NumberFormatException e)
        {
            return Optional.empty();
        }
    }

    public void setLineNumber(String lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public String getFullyQualifiedName()
    {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName)
    {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getProjectLabel()
    {
        return projectLabel;
    }

    public void setProjectLabel(String projectVersion)
    {
        this.projectLabel = projectVersion;
    }

    public IssueClass getIssueClass()
    {
        return issueClass;
    }

    public void setIssueClass(IssueClass className)
    {
        this.issueClass = className;
    }

    public IssueMethod getMethod()
    {
        return method;
    }

    public void setMethod(IssueMethod methodName)
    {
        this.method = methodName;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Issue) obj;
        return Objects.equals(this.issueType, that.issueType) &&
                Objects.equals(this.lineNumber, that.lineNumber) &&
                Objects.equals(this.fullyQualifiedName, that.fullyQualifiedName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(issueType, lineNumber, fullyQualifiedName);
    }

}
