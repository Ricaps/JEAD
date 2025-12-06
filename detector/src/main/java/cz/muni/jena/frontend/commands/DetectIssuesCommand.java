package cz.muni.jena.frontend.commands;

import com.github.javaparser.utils.SourceRoot;
import cz.muni.jena.configuration.Configuration;
import cz.muni.jena.inference.InferenceFacade;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueCategory;
import cz.muni.jena.issue.IssueDao;
import cz.muni.jena.issue.IssueMetadataService;
import cz.muni.jena.issue.detectors.compilation_unit.DetectorCombiner;
import cz.muni.jena.issue.detectors.compilation_unit.IssueDetector;
import cz.muni.jena.issue.detectors.compilation_unit.SpecificIssueDetector;
import cz.muni.jena.issue.detectors.project.ProjectIssueDetector;
import cz.muni.jena.parser.AsyncCompilationUnitParser;
import cz.muni.jena.parser.CallbackCombiner;
import cz.muni.jena.parser.IssueDetectorCallback;
import cz.muni.jena.parser.ThreadExecutionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command
@Service
public class DetectIssuesCommand {
    private static final String CONFIG_DESCRIPTION = "Absolute path from which the configuration should be read.";
    private static final String PATH_DESCRIPTION = "Absolute path to project you wish to analyze";
    private static final String CATEGORIES_FILTER_DESCRIPTION = "You can use this filter to detect only some type of issues. " +
            "Possible values: DI, SECURITY, PERSISTENCE, MOCKING, SERVICE_LAYER";
    private static final String SHOW_THREADS_DESCRIPTION = "Jena should how is load distributed between thread if this attribute is true";
    private static final String LABEL_DESCRIPTION = "Jena will assign label to all anti-patterns, classes and methods found. " +
            "Their label is important for other commands. For more information see their descriptions.";
    private static final String DETECT_ISSUE_DESCRIPTION = "Detect issues command detects issues in project in absolute path p and at the same time it collect extra information about the project such as classes and methods.";
    private static final String USE_MACHINE_LEARNING = "Use machine learning to improve detection";
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectIssuesCommand.class);
    private final List<SpecificIssueDetector> compilationUnitIssueDetectors;
    private final List<ProjectIssueDetector> projectIssueDetectors;
    private final IssueDao issueDao;
    private final IssueMetadataService issueMetadataService;
    private final InferenceFacade inferenceFacade;

    @Inject
    public DetectIssuesCommand(
            List<SpecificIssueDetector> compilationUnitIssueDetectors,
            List<ProjectIssueDetector> projectIssueDetectors,
            IssueDao issueDao,
            IssueMetadataService issueMetadataService,
            InferenceFacade inferenceFacade
    ) {
        this.compilationUnitIssueDetectors = compilationUnitIssueDetectors;
        this.projectIssueDetectors = projectIssueDetectors;
        this.issueDao = issueDao;
        this.issueMetadataService = issueMetadataService;
        this.inferenceFacade = inferenceFacade;
    }

    @Command(command = "detectIssues", description = DETECT_ISSUE_DESCRIPTION)
    public String detectIssues(
            @Option(longNames = "config", shortNames = 'c', description = CONFIG_DESCRIPTION) String configPath,
            @Option(longNames = "projectPath", shortNames = 'p', required = true, description = PATH_DESCRIPTION) String path,
            @Option(longNames = "issueCategory", shortNames = 'i', description = CATEGORIES_FILTER_DESCRIPTION) IssueCategory issueCategory,
            @Option(longNames = "showThreadsRuntime", shortNames = 'd', defaultValue = "false", description = SHOW_THREADS_DESCRIPTION) boolean showThreadsRuntime,
            @Option(longNames = "projectLabel", shortNames = 'l', defaultValue = "0", description = LABEL_DESCRIPTION) String projectLabel,
            @Option(longNames = "machineLearning", shortNames = 'm', defaultValue = "true", description = USE_MACHINE_LEARNING) boolean useMachineLearning
    ) {
        Configuration configuration = Optional.ofNullable(configPath)
                .map(this::loadConfiguration)
                .orElse(Configuration.readConfiguration());
        Set<IssueCategory> issueDetectorFilter = Optional.ofNullable(issueCategory)
                .map(Set::of)
                .orElse(Arrays.stream(IssueCategory.values()).collect(Collectors.toSet()));

        List<SpecificIssueDetector> staticIssueDetectors = compilationUnitIssueDetectors.stream()
                .filter(issueDetector -> issueDetectorFilter.contains(issueDetector.getIssueCategory()))
                .toList();
        List<IssueDetector> detectors = new ArrayList<>(staticIssueDetectors);

        IssueDetector combinedIssueDetector = new DetectorCombiner(detectors);
        List<Issue> issues = Collections.synchronizedList(new ArrayList<>());
        IssueDetectorCallback staticDetectionCallback = new IssueDetectorCallback(
                combinedIssueDetector,
                configuration,
                issues,
                projectLabel,
                issueMetadataService
        );

        List<SourceRoot.Callback> callbackList = new ArrayList<>();
        callbackList.add(staticDetectionCallback);
        try {
            inferenceFacade.startMachineLearningEvaluation(useMachineLearning, issueDetectorFilter, configuration)
                    .ifPresent(callbackList::add);

            CallbackCombiner callbackCombiner = new CallbackCombiner(callbackList);
            new AsyncCompilationUnitParser(path).processCompilationUnits(callbackCombiner);
        } finally {
            List<Issue> mlIssues = inferenceFacade.endMachineLearningEvaluation(useMachineLearning, projectLabel);
            issues.addAll(mlIssues);
        }

        issues.addAll(
                projectIssueDetectors.stream()
                        .filter(issueDetector -> issueDetectorFilter.contains(issueDetector.getIssueCategory()))
                        .flatMap(projectIssueDetector -> projectIssueDetector.findIssues(path, configuration))
                        .toList()
        );
        issues.forEach(issue -> issue.setProjectLabel(projectLabel));
        issues.forEach(this::saveIssue);

        return prepareIssuesAsString(issues)
                + (showThreadsRuntime ? prepareThreadExecutionLogs(staticDetectionCallback.getThreadExecutionLogs()) : "");
    }

    private void saveIssue(Issue issue) {
        try {
            Optional<Issue> savedIssue = issueDao.findOne(Example.of(
                    issue,
                    ExampleMatcher.matchingAll()
            ));
            savedIssue.ifPresent(persistedIssue -> issue.setId(persistedIssue.getId()));
            issueDao.save(issue);
        } catch (Exception e) {
            LOGGER.atWarn().log("We failed to save following issue: " + issue, e);
        }
    }

    private String prepareIssuesAsString(List<Issue> issues) {
        String[][] rows = new String[issues.size() + 1][];
        rows[0] = new String[]{"Issue type ", "Line number ", "Class fully qualified name ", "Method name "};
        int i = 1;
        List<String[]> rowsWithoutHeader = issues.stream().sorted(Comparator.comparing(Issue::getIssueType))
                .map(Issue::toTableRow)
                .toList();
        for (String[] row : rowsWithoutHeader) {
            rows[i] = row;
            i += 1;
        }
        return "We found following issues: " + System.lineSeparator() +
                new TableBuilder(new ArrayTableModel(rows)).build().render(150);
    }

    private String prepareThreadExecutionLogs(List<ThreadExecutionLog> threadExecutionLogs) {
        return "Sorted log of threads running for more then 100 ms:" +
                mapToStringAndJoin(threadExecutionLogs.stream()
                        .filter(threadExecutionLog -> threadExecutionLog.classesOrInterfacesAnalysed() > 0)
                        .filter(threadExecutionLog -> threadExecutionLog.runningTimeInMilliseconds() > 100)
                        .sorted(Comparator.comparing(ThreadExecutionLog::runningTimeInMilliseconds)));
    }

    private String mapToStringAndJoin(Stream<?> objects) {
        return objects.map(Object::toString)
                .collect(Collectors.joining("," + System.lineSeparator()));
    }

    public Configuration loadConfiguration(String configPath) {
        return Configuration.readConfiguration(configPath)
                .orElseThrow(
                        () -> new IllegalArgumentException("There was a problem with loading of custom configuration.")
                );
    }
}
