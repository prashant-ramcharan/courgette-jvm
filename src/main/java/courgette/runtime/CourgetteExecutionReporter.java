package courgette.runtime;

import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class CourgetteExecutionReporter {
    private final StringBuilder executionLog;

    public CourgetteExecutionReporter(StringBuilder executionLog) {
        this.executionLog = executionLog;
    }

    public void createReport(Integer totalFeatures,
                             Integer featuresPassed,
                             Integer featuresFailed,
                             Integer featuresRerun,
                             Integer featuresRerunPassed,
                             Instant sessionStart) {

        executionLog.insert(0, "Courgette-JVM Execution Report\n==============================\n");

        executionLog.insert(executionLog.length(), "\n\nReport Summary\n--------------\n");
        executionLog.append(String.format("Total Features: %s\n", totalFeatures));
        executionLog.append(String.format("Passed: %s\n", featuresPassed));
        executionLog.append(String.format("Failed: %s\n", featuresFailed));

        if (featuresRerun > 0) {
            executionLog.append(String.format("Rerun: %s\n", featuresRerun));
            executionLog.append(String.format("Passed After Rerun: %s\n", featuresRerunPassed));
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