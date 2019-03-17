package courgette.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.runtime.utils.FileUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourgetteRunner {
    private final List<Callable<Boolean>> runners = new ArrayList<>();
    private final CopyOnWriteArrayList<String> reruns = new CopyOnWriteArrayList<>();
    private final Map<String, CopyOnWriteArrayList<String>> reports = new HashMap<>();

    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRuntimeOptions defaultRuntimeOptions;
    private final boolean rerunFailedScenarios;

    private final List<CourgetteRunResult> runResults = new ArrayList<>();

    private final boolean canRunFeatures;

    public CourgetteRunner(List<CourgetteRunnerInfo> runnerInfoList, CourgetteProperties courgetteProperties) {
        this.runnerInfoList = runnerInfoList;
        this.canRunFeatures = runnerInfoList.size() > 0;
        this.courgetteProperties = courgetteProperties;
        this.rerunFailedScenarios = courgetteProperties.getCourgetteOptions().rerunFailedScenarios();
        this.defaultRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
    }

    public void run() {
        final ExecutorService executor = Executors.newFixedThreadPool(optimizedThreadCount());

        final Queue<CourgetteRunnerInfo> runnerQueue = new ArrayDeque<>(runnerInfoList);

        while (!runnerQueue.isEmpty()) {
            final CourgetteRunnerInfo runnerInfo = runnerQueue.poll();

            final Map<String, List<String>> cucumberArgs = runnerInfo.getRuntimeOptions();

            final String featureUri = cucumberArgs.get(null).get(0);

            this.runners.add(() -> {
                try {
                    boolean isPassed = runFeature(cucumberArgs);

                    if (isPassed) {
                        runResults.add(new CourgetteRunResult(featureUri, CourgetteRunResult.Status.PASSED));
                        return true;
                    }

                    String rerunFile = runnerInfo.getRerunFile();
                    String rerun = FileUtils.readFile(rerunFile, false);

                    if (rerunFailedScenarios && rerun != null) {
                        final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerun);

                        final String rerunFeatureUri = rerunCucumberArgs.get(null).get(0);

                        runResults.add(new CourgetteRunResult(rerunFeatureUri, CourgetteRunResult.Status.RERUN));

                        int rerunAttempts = courgetteProperties.getCourgetteOptions().rerunAttempts();

                        rerunAttempts = rerunAttempts < 1 ? 1 : rerunAttempts;

                        while (rerunAttempts-- > 0) {
                            isPassed = runFeature(rerunCucumberArgs);

                            if (isPassed) {
                                runResults.add(new CourgetteRunResult(rerunFeatureUri, CourgetteRunResult.Status.PASSED_AFTER_RERUN));
                                return true;
                            }
                        }
                        runResults.add(new CourgetteRunResult(rerunFeatureUri, CourgetteRunResult.Status.FAILED));
                    } else {
                        runResults.add(new CourgetteRunResult(featureUri, CourgetteRunResult.Status.FAILED));
                    }

                    if (rerun != null) {
                        reruns.add(rerun);
                    }
                } finally {
                    runnerInfo.getReportFiles().forEach(reportFile -> {
                        if (reportFile.contains(courgetteProperties.getSessionId())) {
                            boolean isJson = reportFile.endsWith(".json");

                            String report = isJson
                                    ? prettyJson(FileUtils.readFile(reportFile, true))
                                    : FileUtils.readFile(reportFile, true);

                            final CopyOnWriteArrayList<String> reportDetails = new CopyOnWriteArrayList<>();
                            reportDetails.add(report);

                            reports.put(reportFile, reportDetails);
                        }
                    });
                }
                return false;
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

    public void createCourgetteReport() {
        final CourgetteHtmlReporter courgetteReport = new CourgetteHtmlReporter(courgetteProperties, runResults, defaultRuntimeOptions.getCourgetteReportJson());
        courgetteReport.create();
    }

    public boolean hasFailures() {
        return runResults.stream().anyMatch(result -> result.getStatus() == CourgetteRunResult.Status.FAILED);
    }

    public boolean canRunFeatures() {
        return canRunFeatures;
    }

    private boolean runFeature(Map<String, List<String>> args) {
        try {
            final boolean showTestOutput = courgetteProperties.getCourgetteOptions().showTestOutput();
            return 0 == new CourgetteFeatureRunner(args, showTestOutput).run();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
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
}