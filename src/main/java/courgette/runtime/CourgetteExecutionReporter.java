package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class CourgetteExecutionReporter {
    private final StringBuilder executionLog;
    private final CourgetteRunLevel courgetteRunLevel;

    public CourgetteExecutionReporter(StringBuilder executionLog, CourgetteRunLevel courgetteRunLevel) {
        this.executionLog = executionLog;
        this.courgetteRunLevel = courgetteRunLevel;
    }

    public void createReport(Integer total,
                             Integer passed,
                             Integer failed,
                             Integer rerun,
                             Integer rerunPassed,
                             Instant sessionStart) {

        executionLog.insert(0, "Courgette-JVM Execution Report\n==============================\n");

        executionLog.insert(executionLog.length(), "\n\nReport Summary\n--------------\n");

        if (courgetteRunLevel.equals(CourgetteRunLevel.FEATURE)) {
            executionLog.append(String.format("Total Features: %s\n", total));
        } else {
            executionLog.append(String.format("Total Scenarios: %s\n", total));
        }

        executionLog.append(String.format("Passed: %s\n", passed));
        executionLog.append(String.format("Failed: %s\n", failed));

        if (rerun > 0) {
            executionLog.append(String.format("Rerun: %s\n", rerun));
            executionLog.append(String.format("Passed After Rerun: %s\n", rerunPassed));
        }

        final long elapsedMill = (Instant.now().minus(sessionStart.toEpochMilli(), ChronoUnit.MILLIS)).toEpochMilli();

        String elapsedTime = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedMill),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMill))
        );

        executionLog.append(String.format("\nExecution time: %s", elapsedTime));

        final File targetDir = new File("target");

        if (!targetDir.exists()) {
            if (!targetDir.mkdir()) {
                System.err.println("Unable to create the '/target' directory");
            }
        }
        FileUtils.writeFile("target/courgette-execution-report.txt", executionLog.toString());
    }
}