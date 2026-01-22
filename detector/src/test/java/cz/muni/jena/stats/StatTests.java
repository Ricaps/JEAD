package cz.muni.jena.stats;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatTests
{
    public static final double[][] STRAIT_LINE = new double[][]{{1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}};
    public static final double[][] STRAIT_LINES = new double[][]{{1, 1, 1}, {2, 2, 2}, {3, 3, 3}, {4, 4, 4}, {5, 5, 5}};
    public static final double[][] BAD_CORRELATION = new double[][]{{1, 0}, {2, 3}, {3, -5}, {4, 1}, {5, 20}};
    public static final double[][] BAD_CORRELATIONS = new double[][]{{1, 0, 2}, {2, 3, 0}, {3, -5, 1}, {4, 1, 0}, {5, 20, 1}};

    @Test
    void simpleRegressionGoodCorrelationTest()
    {

        SimpleRegression regression = new SimpleRegression();
        regression.addData(STRAIT_LINE);
        assertThat(regression.getSignificance()).isZero();
    }

    @Test
    void spearmanGoodCorrelationTest()
    {
        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(STRAIT_LINE);
        assertThat(new SpearmansCorrelation(matrix).getRankCorrelation().getCorrelationPValues().getData()[0][1])
                .isZero();
    }

    @Test
    void simpleRegressionBadCorrelationTest()
    {

        SimpleRegression regression = new SimpleRegression();
        regression.addData(BAD_CORRELATION);
        assertThat(regression.getSignificance()).isGreaterThan(0.05);
    }

    @Test
    void spearmanBadCorrelationTest()
    {
        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(BAD_CORRELATION);
        assertThat(new SpearmansCorrelation(matrix).getRankCorrelation().getCorrelationPValues().getData()[0][1])
                .isGreaterThan(0.05);
    }

    @Test
    void spearmanMultipleVariableGoodCorrelationTest()
    {
        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(STRAIT_LINES);
        double[][] pValues = new SpearmansCorrelation(matrix).getRankCorrelation().getCorrelationPValues().getData();
        assertThat(pValues).isEqualTo(new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
    }

    @Test
    void spearmanMultipleVariableBadCorrelationTest()
    {
        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(BAD_CORRELATIONS);
        double[][] pValues = new SpearmansCorrelation(matrix).getRankCorrelation().getCorrelationPValues().getData();
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                if (i == j)
                {
                    assertThat(pValues[i][j]).isZero();
                } else
                {
                    assertThat(pValues[i][j]).isGreaterThan(0.05);
                }
            }
        }
    }
}
