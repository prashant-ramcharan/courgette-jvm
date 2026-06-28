package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CucumberOptions;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourgetteTestStatisticsTest {
    @Test
    public void calculatesTotalsPercentagesAndRerunCounts() {
        CourgetteTestStatistics statistics = new CourgetteTestStatistics();

        statistics.calculate(Arrays.asList(
                result(CourgetteRunResult.Status.PASSED),
                result(CourgetteRunResult.Status.FAILED),
                result(CourgetteRunResult.Status.RERUN),
                result(CourgetteRunResult.Status.PASSED_AFTER_RERUN)
        ), new CourgetteProperties(new CourgetteRunOptions(RerunRunner.class), "session", 2));

        assertEquals(3, statistics.total());
        assertEquals(2, statistics.passed());
        assertEquals(1, statistics.failed());
        assertEquals(1, statistics.rerun());
        assertEquals(1, statistics.passedAfterRerun());
        assertEquals("66.67", statistics.passedPercentage());
        assertEquals("33.33", statistics.failedPercentage());
        assertTrue(statistics.hasFailures());
    }

    @Test
    public void usesZeroDurationBeforeCalculation() {
        assertEquals("0 min, 0 sec", new CourgetteTestStatistics().duration());
        assertFalse(new CourgetteTestStatistics().hasFailures());
    }

    private CourgetteRunResult result(CourgetteRunResult.Status status) {
        return new CourgetteRunResult(null, null, "feature", status);
    }

    @CourgetteOptions(rerunFailedScenarios = true, cucumberOptions = @CucumberOptions(features = "features"))
    private static class RerunRunner {
    }
}
