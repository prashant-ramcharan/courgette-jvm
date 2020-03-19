package courgette.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.integration.extentreports.ExtentReportsBuilder;
import courgette.integration.extentreports.ExtentReportsProperties;
import courgette.integration.reportportal.ReportPortalProperties;
import courgette.integration.reportportal.ReportPortalService;
import courgette.runtime.report.JsonReportParser;
import courgette.runtime.report.model.Feature;
import courgette.runtime.utils.FileUtils;
import io.cucumber.core.feature.CucumberFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CourgetteRunner {
    private final List<Callable<Boolean>> runners = new ArrayList<>();
    private final CopyOnWriteArrayList<String> reruns = new CopyOnWriteArrayList<>();
    private final Map<String, CopyOnWriteArrayList<String>> reports = new HashMap<>();
    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRuntimeOptions defaultRuntimeOptions;
    private final List<CourgetteRunResult> runResults = new ArrayList<>();
    private final boolean rerunFailedScenarios;
    private final boolean canRunFeatures;

    private List<Feature> reportFeatures = new ArrayList<>();

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

        Path customClassPath = null;
        if (shouldUseCustomClassPath()) {
            customClassPath = FileUtils.copyClassPathFilesToTempDirectory();
        }

        while (!runnerQueue.isEmpty()) {
            final CourgetteRunnerInfo runnerInfo = runnerQueue.poll();

            final Map<String, List<String>> cucumberArgs = runnerInfo.getRuntimeOptions();

            final CucumberFeature cucumberFeature = runnerInfo.getCucumberFeature();
            final Integer lineId = runnerInfo.getLineId();
            final String featureUri = cucumberArgs.get(null).get(0);
            final Path finalCustomClassPath = customClassPath;

            this.runners.add(() -> {
                try {
                    boolean isPassed = runFeature(cucumberArgs, finalCustomClassPath);

                    if (isPassed) {
                        runResults.add(new CourgetteRunResult(cucumberFeature, lineId, featureUri, CourgetteRunResult.Status.PASSED));
                        return true;
                    }

                    String rerunFile = runnerInfo.getRerunFile();
                    String rerun = FileUtils.readFile(rerunFile, false);

                    if (rerunFailedScenarios && rerun != null) {
                        final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerun);

                        final String rerunFeatureUri = rerunCucumberArgs.get(null).get(0);

                        runResults.add(new CourgetteRunResult(cucumberFeature, lineId, rerunFeatureUri, CourgetteRunResult.Status.RERUN));

                        int rerunAttempts = courgetteProperties.getCourgetteOptions().rerunAttempts();

                        rerunAttempts = rerunAttempts < 1 ? 1 : rerunAttempts;

                        while (rerunAttempts-- > 0) {
                            isPassed = runFeature(rerunCucumberArgs, finalCustomClassPath);

                            if (isPassed) {
                                runResults.add(new CourgetteRunResult(cucumberFeature, lineId, rerunFeatureUri, CourgetteRunResult.Status.PASSED_AFTER_RERUN));
                                return true;
                            }
                        }
                        runResults.add(new CourgetteRunResult(cucumberFeature, lineId, rerunFeatureUri, CourgetteRunResult.Status.FAILED));
                    } else {
                        runResults.add(new CourgetteRunResult(cucumberFeature, lineId, featureUri, CourgetteRunResult.Status.FAILED));
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
            CourgetteReporter reporter = new CourgetteReporter(reportFile, reports, courgetteProperties);
            boolean mergeTestCaseName = courgetteProperties.isReportPortalPluginEnabled() && reportFile.equalsIgnoreCase(defaultRuntimeOptions.getCourgetteReportXmlForReportPortal());
            reporter.createReport(mergeTestCaseName);
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
        reportFeatures = JsonReportParser.create(new File(defaultRuntimeOptions.getCourgetteReportJson())).getReportFeatures();
        final CourgetteHtmlReporter courgetteReport = new CourgetteHtmlReporter(courgetteProperties, runResults, reportFeatures);
        courgetteReport.create();
    }

    public void createCourgetteExtentReports() {
        final ExtentReportsProperties extentReportsProperties = new ExtentReportsProperties(courgetteProperties);
        final boolean isStrict = courgetteProperties.getCourgetteOptions().cucumberOptions().strict();
        final ExtentReportsBuilder extentReportsBuilder = ExtentReportsBuilder.create(extentReportsProperties, reportFeatures, isStrict);
        extentReportsBuilder.buildReport();
    }

    public boolean hasFailures() {
        return runResults.stream().anyMatch(result -> result.getStatus() == CourgetteRunResult.Status.FAILED);
    }

    public List<CourgetteRunResult> getFailures() {
        return runResults.stream().filter(t -> t.getStatus() == CourgetteRunResult.Status.FAILED).collect(Collectors.toList());
    }

    public boolean canRunFeatures() {
        return canRunFeatures;
    }

    public void publishReportToReportPortal() {
        try {
            ReportPortalService.create(ReportPortalProperties.getInstance()).publishReport(defaultRuntimeOptions.getCourgetteReportXmlForReportPortal());
        } catch (Exception ex) {
            System.err.format("There was a problem publishing the report to report portal, reason: %s", ex.getMessage());
        }
    }

    private boolean runFeature(Map<String, List<String>> args, Path customClassPath) {
        try {
            final boolean showTestOutput = courgetteProperties.getCourgetteOptions().showTestOutput();
            return 0 == new CourgetteFeatureRunner(args, showTestOutput, customClassPath).run();
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

    private boolean shouldUseCustomClassPath() {
        return isJava8() && courgetteProperties.getCourgetteOptions().shortenJavaClassPath();
    }

    private boolean isJava8() {
        return System.getProperty("java.version").startsWith("1.8");
    }
}