package cz.muni.jena.frontend.commands.antipattern.correlation;

import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueDao;
import cz.muni.jena.issue.IssueType;
import cz.muni.jena.issue.IssueTypeComparator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.round;

@Service
public class AntipatternCorrelationTestService
{
    private static final List<IssueType> ISSUE_TYPES = Arrays.stream(IssueType.values()).sorted(new IssueTypeComparator()).toList();
    private final IssueDao issueDao;

    @Inject
    public AntipatternCorrelationTestService(IssueDao issueDao)
    {
        this.issueDao = issueDao;
    }

    public String analyseAntipatternCorrelation()
    {
        List<Issue> issues = issueDao.findAll();
        List<Map<IssueType, Integer>> issueTypesCounts = issues.stream().collect(Collectors.toMap(
                Issue::getProjectLabel,
                Stream::of,
                Stream::concat
        )).values().stream().map(issueStream -> issueStream.collect(Collectors.toMap(
                Issue::getIssueType,
                issue -> 1,
                Integer::sum
        ))).toList();
        double[][] issueTypesCountsArray = new double[issueTypesCounts.size()][];
        for (int i = 0; i < issueTypesCounts.size(); i++)
        {
            issueTypesCountsArray[i] = mapToArray(issueTypesCounts.get(i));
        }
        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(issueTypesCountsArray);
        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation(matrix);
        PearsonsCorrelation rankCorrelation = spearmansCorrelation.getRankCorrelation();
        double[][] pValues = roundAll(rankCorrelation.getCorrelationPValues().getData());
        double[][] correlationValues = roundAll(rankCorrelation.getCorrelationMatrix().getData());
        return "pValues: " + System.lineSeparator() +
                presentData(pValues) + System.lineSeparator() +
                "correlations: " + System.lineSeparator()
                + presentData(correlationValues);
    }

    private String presentData(double[][] data)
    {
        return ISSUE_TYPES.stream().map(Object::toString).collect(Collectors.joining("\t"))
                + System.lineSeparator()
                + Arrays.stream(data)
                .map(row -> Arrays.stream(row)
                        .boxed()
                        .map(Object::toString)
                        .collect(Collectors.joining("\t"))
                )
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private double[] mapToArray(Map<IssueType, Integer> map)
    {
        double[] array = new double[ISSUE_TYPES.size()];
        for (int i = 0; i < ISSUE_TYPES.size(); i++)
        {
            array[i] = Optional.ofNullable(map.get(ISSUE_TYPES.get(i))).orElse(0);
        }
        return array;
    }

    private double[][] roundAll(double[][] data)
    {
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[i].length; j++)
            {
                data[i][j] = round(data[i][j] * 1000) / 1000.0;
            }
        }
        return data;
    }
}
