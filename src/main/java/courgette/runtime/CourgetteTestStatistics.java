package courgette.runtime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CourgetteTestStatistics {
    private int total;
    private int passed;
    private int failed;
    private int rerun;
    private int passedAfterRerun;
    private String duration;

    private static CourgetteTestStatistics instance;

    public static CourgetteTestStatistics current() {
        if (instance == null) {
            instance = new CourgetteTestStatistics();
        }
        return instance;
    }

    protected void calculate(List<CourgetteRunResult> runResults, CourgetteProperties courgetteProperties) {
        calculateTestStatistics(runResults, courgetteProperties);
    }

    public int total() {
        return total;
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public int rerun() {
        return rerun;
    }

    public String duration() {
        return duration == null ? "0 min, 0 sec" : duration;
    }

    public int passedAfterRerun() {
        return passedAfterRerun;
    }

    public int passedPercentage() {
        return calculatePercentage(passed);
    }

    public int failedPercentage() {
        return calculatePercentage(failed);
    }

    public boolean hasFailures() {
        return failed > 0;
    }

    private void calculateTestStatistics(List<CourgetteRunResult> runResults, CourgetteProperties courgetteProperties) {
        total = (int) runResults.stream().filter(result -> !result.getStatus().equals(CourgetteRunResult.Status.RERUN)).count();

        passed = calculateStatus(runResults, CourgetteRunResult.Status.PASSED);

        failed = total - passed;

        if (courgetteProperties.getCourgetteOptions().rerunFailedScenarios()) {
            rerun = calculateStatus(runResults, CourgetteRunResult.Status.RERUN);
            passedAfterRerun = calculateStatus(runResults, CourgetteRunResult.Status.PASSED_AFTER_RERUN);
        }

        final long elapsedMill = (Instant.now().minus(courgetteProperties.getSessionStartTime().toEpochMilli(), ChronoUnit.MILLIS)).toEpochMilli();

        duration = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedMill),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMill)));
    }

    private int calculateStatus(List<CourgetteRunResult> runResults, CourgetteRunResult.Status status) {
        return (int) runResults.stream().filter(result -> result.getStatus().equals(status)).count();
    }

    private int calculatePercentage(double value) {
        return (int) Math.round(value / total * 100);
    }
}
