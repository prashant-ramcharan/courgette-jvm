package courgette.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.utils.FileUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CourgetteRunner {
    private final List<Callable<Boolean>> runners = new ArrayList<>();
    private final CopyOnWriteArrayList<String> reruns = new CopyOnWriteArrayList<>();
    private final Map<String, CopyOnWriteArrayList<String>> reports = new HashMap<>();

    private final Integer total;
    private final AtomicInteger passed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger rerun = new AtomicInteger(0);
    private final AtomicInteger rerunPassed = new AtomicInteger(0);

    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRuntimeOptions defaultRuntimeOptions;
    private final Boolean rerunFailedScenarios;

    private final StringBuilder executionLog = new StringBuilder();

    public CourgetteRunner(List<CourgetteRunnerInfo> runnerInfoList, CourgetteProperties courgetteProperties) {
        this.runnerInfoList = runnerInfoList;
        this.courgetteProperties = courgetteProperties;
        this.rerunFailedScenarios = courgetteProperties.getCourgetteOptions().rerunFailedScenarios();
        this.total = runnerInfoList.size();
        this.defaultRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
    }

    public void run() {
        final ExecutorService executor = Executors.newFixedThreadPool(optimizedThreadCount());

        final Queue<CourgetteRunnerInfo> runnerQueue = new ArrayDeque<>(runnerInfoList);

        while (!runnerQueue.isEmpty()) {
            final CourgetteRunnerInfo runnerInfo = runnerQueue.poll();

            final Map<String, List<String>> cucumberArgs = runnerInfo.getRuntimeOptions();

            this.runners.add(() -> {
                try {
                    Boolean isPassed = runFeature("Running feature", runnerInfo, cucumberArgs);

                    if (isPassed) {
                        passed.incrementAndGet();
                        return Boolean.TRUE;
                    }

                    failed.incrementAndGet();

                    String rerunFile = runnerInfo.getRerunFile();
                    String rerun = FileUtils.readFile(rerunFile, Boolean.FALSE);

                    if (rerunFailedScenarios && rerun != null) {
                        final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerun);

                        isPassed = runFeature(String.format("Re-running failed scenario '%s'", rerun), null, rerunCucumberArgs);

                        this.rerun.incrementAndGet();

                        if (isPassed) {
                            failed.decrementAndGet();
                            passed.incrementAndGet();
                            rerunPassed.incrementAndGet();
                            return Boolean.TRUE;
                        }
                    }
                    if (rerun != null) {
                        reruns.add(rerun);
                    }
                } finally {
                    runnerInfo.getReportFiles().forEach(reportFile -> {
                        if (reportFile.contains(courgetteProperties.getSessionId())) {
                            Boolean isJson = reportFile.endsWith(".json");

                            String report = isJson
                                    ? prettyJson(FileUtils.readFile(reportFile, Boolean.TRUE))
                                    : FileUtils.readFile(reportFile, Boolean.TRUE);

                            final CopyOnWriteArrayList<String> reportDetails = new CopyOnWriteArrayList<>();
                            reportDetails.add(report);

                            reports.put(reportFile, reportDetails);
                        }
                    });
                }
                return Boolean.FALSE;
            });
        }

        try {
            executor.invokeAll(runners);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }

    public void createReport() {
        final List<String> reportFiles = defaultRuntimeOptions.getReportJsFiles();

        reportFiles.forEach(reportFile -> {
            CourgetteReporter reporter = new CourgetteReporter(reportFile, reports);
            reporter.createReport();
        });
    }

    public void createRerunFile() {
        final List<String> rerun = new ArrayList<>();

        reruns.sort(String::compareTo);
        reruns.forEach(rerun::add);

        final String rerunFile = defaultRuntimeOptions.getCucumberRerunFile();

        if (rerunFile != null) {
            FileUtils.writeFile(rerunFile, rerun);
        }
    }

    public void createExecutionReport() {
        final CourgetteExecutionReporter executionReporter = new CourgetteExecutionReporter(executionLog, courgetteProperties.getCourgetteOptions().runLevel());

        executionReporter.createReport(
                total,
                passed.get(),
                failed.get(),
                rerun.get(),
                rerunPassed.get(),
                courgetteProperties.getSessionStartTime());
    }

    public Boolean allFeaturesPassed() {
        return failed.get() == 0;
    }

    private Boolean runFeature(String msg, CourgetteRunnerInfo runnerInfo, Map<String, List<String>> args) throws IOException {
        try {
            final Boolean showTestOutput = courgetteProperties.getCourgetteOptions().showTestOutput();

            final Boolean result = 0 == new CourgetteFeatureRunner(args, showTestOutput).run();

            addResultToExecutionLog(msg, runnerInfo, result);
            return result;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Boolean.FALSE;
        }
    }

    private String prettyJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final Object jsonObject = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (IOException e) {
            return json;
        }
    }

    private Integer optimizedThreadCount() {
        return courgetteProperties.getMaxThreads() > runnerInfoList.size()
                ? runnerInfoList.size()
                : courgetteProperties.getMaxThreads() < 1
                ? 1
                : courgetteProperties.getMaxThreads();
    }

    private void addResultToExecutionLog(String msg, CourgetteRunnerInfo runnerInfo, Boolean result) {
        if (runnerInfo == null) {
            executionLog.append(String.format("\n[THREAD-%s] %s -> %s",
                    Thread.currentThread().getId(),
                    msg,
                    result ? "PASSED" : "FAILED"));
            return;
        }

        if (runnerInfo.getCourgetteRunLevel().equals(CourgetteRunLevel.FEATURE)) {
            executionLog.append(String.format("\n[THREAD-%s] %s: '%s' -> %s",
                    Thread.currentThread().getId(),
                    msg,
                    runnerInfo.getCucumberFeature().getGherkinFeature().getName(),
                    result ? "PASSED" : "FAILED"));
        } else {
            executionLog.append(String.format("\n[THREAD-%s] %s: '%s' at scenario line number '%s' -> %s",
                    Thread.currentThread().getId(),
                    msg,
                    runnerInfo.getCucumberFeature().getGherkinFeature().getName(),
                    runnerInfo.getLineId(),
                    result ? "PASSED" : "FAILED"));
        }
    }
}