package courgette.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.utils.FileUtils;
import cucumber.runtime.model.CucumberFeature;

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
    private final Boolean rerunFailedScenarios;

    private final List<CourgetteRunResult> runResults = new ArrayList<>();

    public CourgetteRunner(List<CourgetteRunnerInfo> runnerInfoList, CourgetteProperties courgetteProperties) {
        this.runnerInfoList = runnerInfoList;
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

            this.runners.add(() -> {
                try {
                    Boolean isPassed = runFeature(cucumberArgs);

                    if (isPassed) {
                        addRunResult(runnerInfo, CourgetteRunResult.Status.PASSED);
                        return Boolean.TRUE;
                    }

                    String rerunFile = runnerInfo.getRerunFile();
                    String rerun = FileUtils.readFile(rerunFile, Boolean.FALSE);

                    if (rerunFailedScenarios && rerun != null) {
                        final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerun);

                        isPassed = runFeature(rerunCucumberArgs);

                        if (isPassed) {
                            addRunResult(runnerInfo, CourgetteRunResult.Status.PASSED_AFTER_RERUN);
                            return Boolean.TRUE;
                        } else {
                            addRunResult(runnerInfo, CourgetteRunResult.Status.FAILED);
                        }
                    } else {
                        addRunResult(runnerInfo, CourgetteRunResult.Status.FAILED);
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

    public void createCourgetteReport() {
        final CourgetteHtmlReporter courgetteReport = new CourgetteHtmlReporter(courgetteProperties, runResults);
        courgetteReport.create();
    }

    public Boolean allFeaturesPassed() {
        return runResults.stream().noneMatch(result -> result.getStatus() == CourgetteRunResult.Status.FAILED);
    }

    private Boolean runFeature(Map<String, List<String>> args) throws IOException {
        try {
            final Boolean showTestOutput = courgetteProperties.getCourgetteOptions().showTestOutput();
            return 0 == new CourgetteFeatureRunner(args, showTestOutput).run();
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

    private void addRunResult(CourgetteRunnerInfo runInfo, CourgetteRunResult.Status status) {
        final CucumberFeature cucumberFeature = runInfo.getCucumberFeature();

        if (courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE) {
            runResults.add(new CourgetteRunResult(cucumberFeature.getGherkinFeature().getName(), cucumberFeature.getGherkinFeature().getLine(), status));
        } else {
            final String scenarioName = cucumberFeature.getFeatureElements().get(0).getGherkinModel().getName();
            runResults.add(new CourgetteRunResult(scenarioName, runInfo.getLineId(), status));
        }
    }
}