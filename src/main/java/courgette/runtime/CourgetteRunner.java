package courgette.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.api.CourgetteRunLevel;
import courgette.integration.extentreports.ExtentReportsBuilder;
import courgette.integration.extentreports.ExtentReportsProperties;
import courgette.integration.reportportal.ReportPortalProperties;
import courgette.integration.reportportal.ReportPortalService;
import courgette.runtime.report.JsonReportParser;
import courgette.runtime.report.model.Feature;
import courgette.runtime.utils.FileUtils;
import io.cucumber.messages.Messages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.FileUtils.readFile;
import static courgette.runtime.utils.FileUtils.writeFile;

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

    private Map<io.cucumber.core.gherkin.Feature, List<List<Messages.Envelope>>> reportMessages = new HashMap<>();

    private String cucumberReportUrl = "#";

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

            final io.cucumber.core.gherkin.Feature feature = runnerInfo.getFeature();
            final Integer lineId = runnerInfo.getLineId();
            final String featureUri = cucumberArgs.get(null).get(0);

            this.runners.add(() -> {
                try {
                    if (runFeature(cucumberArgs)) {
                        runResults.add(new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.PASSED));
                        return true;
                    }

                    String rerunFile = runnerInfo.getRerunFile();

                    String rerun = readFile(rerunFile, false);

                    if (rerunFailedScenarios && rerun != null) {

                        if (courgetteProperties.isFeatureRunLevel()) {

                            runResults.add(new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.RERUN));

                            if (rerunFeature(cucumberArgs)) {
                                runResults.add(new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.PASSED_AFTER_RERUN));
                                return true;
                            } else {
                                runResults.add(new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.FAILED));
                            }
                        } else {
                            final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerun);

                            final String rerunFeatureUri = rerunCucumberArgs.get(null).get(0);

                            runResults.add(new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.RERUN));

                            if (rerunFeature(rerunCucumberArgs)) {
                                runResults.add(new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.PASSED_AFTER_RERUN));
                                return true;
                            }
                            runResults.add(new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.FAILED));
                        }
                    } else {
                        runResults.add(new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.FAILED));
                    }

                    if (rerun != null) {
                        reruns.add(rerun);
                    }
                } finally {
                    runnerInfo.getReportFiles().forEach(reportFile -> {
                        if (reportFile.contains(courgetteProperties.getSessionId())) {

                            boolean isJson = reportFile.endsWith(".json");

                            String report = isJson
                                    ? prettyJson(readFile(reportFile, true))
                                    : readFile(reportFile, true);

                            boolean isNdJson = reportFile.endsWith(".ndjson");

                            if (isNdJson) {
                                reportMessages.computeIfAbsent(feature, r -> new ArrayList<>())
                                        .addAll(Collections.singleton(CourgetteNdJsonCreator.createMessages(report)));
                            } else {
                                reports.computeIfAbsent(reportFile, r -> new CopyOnWriteArrayList<>()).add(report);
                            }
                        }
                    });
                }
                return false;
            });
        }

        try {
            executor.invokeAll(runners);
        } catch (InterruptedException e) {
            printExceptionStackTrace(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean rerunFeature(Map<String, List<String>> args) {
        int rerunAttempts = courgetteProperties.getCourgetteOptions().rerunAttempts();

        rerunAttempts = Math.max(rerunAttempts, 1);

        while (rerunAttempts-- > 0) {
            if (runFeature(args)) {
                return true;
            }
        }
        return false;
    }

    public void createCucumberReport() {
        final List<String> reportFiles = defaultRuntimeOptions.getReportFiles();

        final CourgetteReporter courgetteReporter = new CourgetteReporter(reports, reportMessages, defaultRuntimeOptions, courgetteProperties);

        reportFiles.forEach(reportFile -> {
            boolean mergeTestCaseName = courgetteProperties.isReportPortalPluginEnabled() && reportFile.equalsIgnoreCase(defaultRuntimeOptions.getCourgetteReportXmlForReportPortal());
            courgetteReporter.createCucumberReport(reportFile, mergeTestCaseName);
        });

        final Optional<String> publishedReport = courgetteReporter.publishCucumberReport();
        publishedReport.ifPresent(reportUrl -> cucumberReportUrl = reportUrl);
    }

    public void createRerunFile() {
        final List<String> rerun = new ArrayList<>();

        reruns.sort(String::compareTo);
        rerun.addAll(reruns);

        final String rerunFile = defaultRuntimeOptions.getCucumberRerunFile();

        if (rerunFile != null) {
            writeFile(rerunFile, String.join("\n", rerun));
        }
    }

    public void createCourgetteReport() {
        try {
            final File reportJson = new File(defaultRuntimeOptions.getCourgetteReportJson());
            final CourgetteRunLevel runLevel = courgetteProperties.getCourgetteOptions().runLevel();

            if (reportJson.exists()) {
                reportFeatures = JsonReportParser.create(reportJson, runLevel).getReportFeatures();

                final CourgetteHtmlReporter courgetteReport = new CourgetteHtmlReporter(courgetteProperties, runResults, reportFeatures, cucumberReportUrl);

                courgetteReport.create();
            }
        } catch (Exception e) {
            printExceptionStackTrace(e);
        }
    }

    public void createCourgetteExtentReports() {
        try {
            final ExtentReportsProperties extentReportsProperties = new ExtentReportsProperties(courgetteProperties);

            final ExtentReportsBuilder extentReportsBuilder = ExtentReportsBuilder.create(extentReportsProperties, reportFeatures);

            extentReportsBuilder.buildReport();
        } catch (Exception e) {
            printExceptionStackTrace(e);
        }
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
            final ReportPortalService service = ReportPortalService.create(ReportPortalProperties.getInstance());

            boolean published = service.publishReport(defaultRuntimeOptions.getCourgetteReportXmlForReportPortal());

            if (published) {
                service.updateLaunchTags();
            }
        } catch (Exception e) {
            printExceptionStackTrace(e);
        }
    }

    public void cleanupCourgetteHtmlReportFiles() {
        FileUtils.deleteDirectorySilently(defaultRuntimeOptions.getCourgetteReportDataDirectory());
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