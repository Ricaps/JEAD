package cz.muni.jena.issue;

import com.github.javaparser.Range;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import cz.muni.jena.issue.language.elements.NodeWrapper;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Range.closed;

@Service
public class IssueMetadataService {

    private final IssueClassDao issueClassDao;
    private final IssueMethodDao issueMethodDao;

    public IssueMetadataService(IssueClassDao issueClassDao, IssueMethodDao issueMethodDao) {
        this.issueClassDao = issueClassDao;
        this.issueMethodDao = issueMethodDao;
    }

    public void setMetaDataToIssues(List<IssueWithLazyMeta> issues, String projectLabel) {
        issues.stream()
                .collect(Collectors.groupingBy(IssueWithLazyMeta::classOrInterfaceDeclaration))
                .forEach(((classOrInterfaceDeclaration, issueWithLazyMetas) ->
                        setMetaDataToIssues(classOrInterfaceDeclaration, issueWithLazyMetas.stream().map(IssueWithLazyMeta::issue).toList(), projectLabel)));
    }

    public void setMetaDataToIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, List<Issue> issuesFound, String projectLabel) {
        AtomicLong classComplexity = new AtomicLong();
        final Optional<IssueClass> issueClass = saveIssueClass(classOrInterfaceDeclaration, projectLabel);
        RangeMap<Integer, IssueMethod> methodMap = TreeRangeMap.create();
        List<? extends CallableDeclaration<? extends CallableDeclaration<?>>> callableDeclarations = Stream.concat(
                classOrInterfaceDeclaration.getMethods().stream(),
                classOrInterfaceDeclaration.findAll(ConstructorDeclaration.class).stream()
        ).toList();

        callableDeclarations
                .forEach(callableDeclaration -> callableDeclaration.getRange()
                        .ifPresent(
                                range ->
                                {
                                    long complexity = new NodeWrapper<>(callableDeclaration).calculateComplexity();
                                    IssueMethod method = new IssueMethod(
                                            projectLabel,
                                            complexity,
                                            callableDeclaration.getNameAsString()
                                    );
                                    issueClass.ifPresent(method::setIssueClass);
                                    addPersistedMethodToMap(methodMap, method, range);
                                    classComplexity.addAndGet(complexity);
                                }
                        ));
        issueClass.ifPresent(issueClass1 -> issueClass1.setComplexity(classComplexity.get()));
        Optional<IssueClass> updatedIssueClass = issueClass.flatMap(this::updateIssueClass);
        issuesFound.forEach(issue ->
        {
            Optional<IssueMethod> method = issue.getLineNumberAsInt()
                    .map(methodMap::get);
            updatedIssueClass.ifPresent(issue::setIssueClass);
            method.ifPresent(issue::setMethod);
        });
    }

    private Optional<IssueClass> saveIssueClass(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, String projectLabel) {
        try {
            ExampleMatcher matcher = ExampleMatcher.matchingAll()
                    .withIgnorePaths("complexity");
            Optional<IssueClass> issueClass = classOrInterfaceDeclaration.getFullyQualifiedName()
                    .map(name -> new IssueClass(projectLabel, 0, name));
            Optional<IssueClass> savedClass = issueClass
                    .flatMap(issueCLass -> issueClassDao.findOne(
                            Example.of(issueCLass, matcher)
                    ));
            if (savedClass.isPresent()) {
                return savedClass;
            }
            return issueClass.map(issueClassDao::save);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<IssueClass> updateIssueClass(IssueClass issueClass) {
        try {
            return Optional.of(issueClassDao.save(issueClass));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void addPersistedMethodToMap(
            RangeMap<Integer, IssueMethod> methodMap,
            IssueMethod issueMethod,
            Range range
    ) {
        try {
            Optional<IssueMethod> saveMethod = issueMethodDao.findOne(Example.of(
                    issueMethod,
                    ExampleMatcher.matchingAll()
                            .withIgnorePaths("complexity")
            ));
            saveMethod.ifPresent(method -> method.setComplexity(issueMethod.getComplexity()));
            IssueMethod resultIssueMethod;
            resultIssueMethod = saveMethod.orElseGet(() -> issueMethodDao.save(issueMethod));
            methodMap.put(
                    closed(range.begin.line, range.end.line),
                    resultIssueMethod
            );
        } catch (Exception ignored) {

        }

    }
}
