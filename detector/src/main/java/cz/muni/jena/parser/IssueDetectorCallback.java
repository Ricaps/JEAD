package cz.muni.jena.parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueMetadataService;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IssueDetectorCallback implements SourceRoot.Callback
{
    private final IssueDetector issueDetector;
    private final Configuration configuration;
    private final List<Issue> issues;
    private final List<ThreadExecutionLog> threadExecutionLogs;
    private final String projectLabel;
    private final IssueMetadataService issueMetadataService;

    public IssueDetectorCallback(
            IssueDetector issueDetector,
            Configuration configuration,
            List<Issue> issues,
            String projectLabel, IssueMetadataService issueMetadataService)
    {
        this.issueDetector = issueDetector;
        this.configuration = configuration;
        this.issues = issues;
        this.projectLabel = projectLabel;
        this.issueMetadataService = issueMetadataService;
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
            issueMetadataService.setMetaDataToIssues(classOrInterfaceDeclaration, issuesFound, projectLabel);

            synchronized (issues)
            {
                issues.addAll(issuesFound);
            }
        }
    }

    public List<ThreadExecutionLog> getThreadExecutionLogs()
    {
        return threadExecutionLogs;
    }
}
