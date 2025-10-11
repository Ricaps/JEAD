package cz.muni.jena.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueClass;
import cz.muni.jena.issue.IssueClassDao;
import cz.muni.jena.issue.IssueMethod;
import cz.muni.jena.issue.IssueMethodDao;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.issue.language.elements.NodeWrapper;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.google.common.collect.Range.closed;

public class IssueDetectorCallback implements SourceRoot.Callback
{
    private final IssueDetector issueDetector;
    private final Configuration configuration;
    private final List<Issue> issues;
    private final List<ThreadExecutionLog> threadExecutionLogs;
    private final IssueMethodDao issueMethodDao;
    private final IssueClassDao issueClassDao;
    private final String projectLabel;

    public IssueDetectorCallback(
            IssueDetector issueDetector,
            Configuration configuration,
            List<Issue> issues,
            IssueMethodDao issueMethodDao,
            IssueClassDao issueClassDao,
            String projectLabel)
    {
        this.issueDetector = issueDetector;
        this.configuration = configuration;
        this.issues = issues;
        this.issueMethodDao = issueMethodDao;
        this.issueClassDao = issueClassDao;
        this.projectLabel = projectLabel;
        threadExecutionLogs = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result)
    {
        result.ifSuccessful(this::process);
        return Result.DONT_SAVE;
    }

    public void process(CompilationUnit compilationUnit)
    {
        long start = System.currentTimeMillis();
        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = compilationUnit
                .findAll(ClassOrInterfaceDeclaration.class);
        int linesTotal = classOrInterfaceDeclarations.stream()
                .map(
                        classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getRange()
                                .map(Range::getLineCount)
                                .orElse(0)
                ).reduce(0, Integer::sum);
        runAnalysis(classOrInterfaceDeclarations);
        logThreadExecutionLog(start, classOrInterfaceDeclarations.size(), linesTotal);
    }

    private void logThreadExecutionLog(long start, int classesOrInterfacesAnalysed, int linesTotal)
    {
        long finish = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean()
                .getThreadInfo(currentThread.getId());
        long timeNotRunning = threadInfo.getBlockedTime() + threadInfo.getWaitedTime();
        long runningTime = finish - start - timeNotRunning;
        synchronized (threadExecutionLogs)
        {
            threadExecutionLogs.add(
                    new ThreadExecutionLog(
                            threadName,
                            runningTime,
                            classesOrInterfacesAnalysed,
                            linesTotal
                    )
            );
        }
    }

    private void runAnalysis(List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations)
    {
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations)
        {
            List<Issue> issuesFound = issueDetector.findIssues(classOrInterfaceDeclaration, configuration).toList();
            setMetaDataToIssues(classOrInterfaceDeclaration, issuesFound);

            synchronized (issues)
            {
                issues.addAll(issuesFound);
            }
        }
    }

    private void setMetaDataToIssues(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, List<Issue> issuesFound)
    {
        AtomicLong classComplexity = new AtomicLong();
        final Optional<IssueClass> issueClass = saveIssueClass(classOrInterfaceDeclaration);
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

    private Optional<IssueClass> saveIssueClass(ClassOrInterfaceDeclaration classOrInterfaceDeclaration)
    {
        try
        {
            ExampleMatcher matcher = ExampleMatcher.matchingAll()
                    .withIgnorePaths("complexity");
            Optional<IssueClass> issueClass = classOrInterfaceDeclaration.getFullyQualifiedName()
                    .map(name -> new IssueClass(projectLabel, 0, name));
            Optional<IssueClass> savedClass = issueClass
                    .flatMap(issueCLass -> issueClassDao.findOne(
                            Example.of(issueCLass, matcher)
                    ));
            if (savedClass.isPresent())
            {
                return savedClass;
            }
            return issueClass.map(issueClassDao::save);
        } catch (Exception ignored)
        {
            return Optional.empty();
        }
    }

    private Optional<IssueClass> updateIssueClass(IssueClass issueClass)
    {
        try
        {
            return Optional.of(issueClassDao.save(issueClass));
        } catch (Exception ignored)
        {
            return Optional.empty();
        }
    }

    private void addPersistedMethodToMap(
            RangeMap<Integer, IssueMethod> methodMap,
            IssueMethod issueMethod,
            Range range
    )
    {
        try
        {
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
        } catch (Exception ignored)
        {

        }

    }

    public List<ThreadExecutionLog> getThreadExecutionLogs()
    {
        return threadExecutionLogs;
    }
}
