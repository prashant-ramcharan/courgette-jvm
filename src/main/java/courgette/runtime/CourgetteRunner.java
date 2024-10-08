package courgette.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import courgette.integration.extentreports.ExtentReportsBuilder;
import courgette.integration.extentreports.ExtentReportsProperties;
import courgette.integration.reportportal.ReportPortalPublisher;
import courgette.integration.slack.SlackPublisher;
import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.event.CourgetteEventHolder;
import courgette.runtime.report.model.Feature;
import courgette.runtime.utils.FileUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.FileUtils.readFile;
import static courgette.runtime.utils.FileUtils.writeFile;

public class CourgetteRunner {
    private final List<Callable<Boolean>> runners = new ArrayList<>();
    private final CopyOnWriteArrayList<String> reruns = new CopyOnWriteArrayList<>();
    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRuntimeOptions defaultRuntimeOptions;
    private final CourgetteTestStatistics testStatistics;
    private final List<CourgetteRun> runs = new ArrayList<>();
    private final List<CourgetteRunResult> runResults = new ArrayList<>();
    private final CourgetteReporter courgetteReporter;
    private final CourgetteRuntimePublisher runtimePublisher;
    private final CourgettePluginService courgettePluginService;
    private final boolean canRunFeatures;
    private final AtomicReference<RunStatus> runStatus = new AtomicReference<>(RunStatus.OK);
    private String cucumberReportUrl = "#";

    public CourgetteRunner(List<CourgetteRunnerInfo> runnerInfoList, CourgetteProperties courgetteProperties) {
        this.runnerInfoList = conditionallySort(runnerInfoList, courgetteProperties);
        this.canRunFeatures = !runnerInfoList.isEmpty();
        this.courgetteProperties = courgetteProperties;
        this.testStatistics = CourgetteTestStatistics.current();
        this.defaultRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        this.runtimePublisher = createRuntimePublisher(courgetteProperties, extractRunnerInfoFeatures());
        this.courgettePluginService = createCourgettePluginService();
        this.courgetteReporter = createCourgetteReporter();
    }

    public RunStatus run() {
        final ExecutorService executor = Executors.newFixedThreadPool(optimizedThreadCount());

        final Queue<CourgetteRunnerInfo> runnerQueue = new ArrayDeque<>(runnerInfoList);

        while (!runnerQueue.isEmpty()) {
            final CourgetteRunnerInfo runnerInfo = runnerQueue.poll();

            final Map<String, List<String>> cucumberArgs = runnerInfo.getRuntimeOptions();

            final io.cucumber.core.gherkin.Feature feature = runnerInfo.getFeature();
            final Integer lineId = runnerInfo.getLineId();
            final String featureUri = cucumberArgs.get(null).get(0);

            this.runners.add(() -> {
                if (runFeature(runnerInfo, cucumberArgs)) {
                    addResultAndPublish(runnerInfo, new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.PASSED));
                    return true;
                }

                if (runnerInfo.allowRerun()) {
                    String rerunFile = runnerInfo.getRerunFile();
                    String rerun = readFile(rerunFile, false);
                    final String rerunFeatureUri = evaluateRerunFeatureUri(rerun, featureUri);

                    final Map<String, List<String>> rerunCucumberArgs = runnerInfo.getRerunRuntimeOptions(rerunFeatureUri);

                    CourgetteRunResult rerunResult = new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.RERUN);
                    runResults.add(rerunResult);

                    if (rerunFeature(runnerInfo, rerunCucumberArgs, rerunResult)) {
                        addResultAndPublish(runnerInfo, new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.PASSED_AFTER_RERUN));
                        return true;
                    } else {
                        addResultAndPublish(runnerInfo, new CourgetteRunResult(feature, lineId, rerunFeatureUri, CourgetteRunResult.Status.FAILED_AFTER_RERUN));
                    }

                    if (rerun != null) {
                        reruns.add(rerun);
                    }
                } else {
                    addResultAndPublish(runnerInfo, new CourgetteRunResult(feature, lineId, featureUri, CourgetteRunResult.Status.FAILED));
                }
                return false;
            });
        }

        try {
            runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_RUN_STARTED));
            executor.invokeAll(runners);
        } catch (InterruptedException e) {
            printExceptionStackTrace(e);
            runStatus.set(RunStatus.ERROR);
        } finally {
            testStatistics.calculate(runResults, courgetteProperties);
            runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_RUN_FINISHED));
            runtimePublisher.publish(createTestRunSummaryEventHolder());
            executor.shutdownNow();
        }

        return runStatus.get();
    }

    public void createCucumberReport() {
        final List<String> reportFiles = defaultRuntimeOptions.getReportFiles();
        final boolean publishReport = courgetteProperties.isCucumberReportPublisherEnabled();
        courgetteReporter.createCucumberReports(reportFiles, publishReport).ifPresent(reportUrl -> cucumberReportUrl = reportUrl);
        courgetteReporter.jsonReportParser().createFeatures();
        courgetteReporter.deleteTemporaryReports();
    }

    public void createRerunFile() {
        reruns.sort(String::compareTo);
        final List<String> rerun = new ArrayList<>(reruns);
        rerun.removeIf(String::isEmpty);

        final String rerunFile = defaultRuntimeOptions.getCucumberRerunFile();

        if (rerunFile != null) {
            writeFile(rerunFile, String.join("\n", rerun));
        }
    }

    public void createCourgetteReport() {
        if (courgetteProperties.isCourgetteHtmlReportEnabled()) {
            try {
                final List<Feature> features = courgetteReporter.jsonReportParser().getFeatures();
                final CourgetteHtmlReporter courgetteReport = new CourgetteHtmlReporter(courgetteProperties, runResults, features, cucumberReportUrl);
                courgetteReport.create(testStatistics);
            } catch (Exception e) {
                printExceptionStackTrace(e);
            }
        }
    }

    public void createErrorReport() {
        if (courgetteReporter.hasErrors()) {
            courgetteReporter.createErrorReport();
        }
    }

    public void createCourgettePluginReports() {
        if (courgetteProperties.isExtentReportsPluginEnabled()) {
            try {
                final List<Feature> features = courgetteReporter.jsonReportParser().getFeatures();
                final ExtentReportsProperties extentReportsProperties = new ExtentReportsProperties(courgetteProperties);
                final ExtentReportsBuilder extentReportsBuilder = ExtentReportsBuilder.create(extentReportsProperties, features);
                extentReportsBuilder.buildReport();
            } catch (Exception e) {
                printExceptionStackTrace(e);
            }
        }
    }

    public void createCourgetteRunLogFile() {
        if (courgetteProperties.getCourgetteOptions().generateCourgetteRunLog()) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                FileUtils.writeFile(defaultRuntimeOptions.getCourgetteRunLogJson(), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(runs));
            } catch (JsonProcessingException e) {
                printExceptionStackTrace(e);
            }
        }
    }

    public List<CourgetteRunResult> getFailures() {
        return runResults.stream()
                .filter(t -> t.getStatus().equals(CourgetteRunResult.Status.FAILED) || t.getStatus().equals(CourgetteRunResult.Status.FAILED_AFTER_RERUN))
                .collect(Collectors.toList());
    }

    public boolean canRunFeatures() {
        return canRunFeatures;
    }

    public void printCourgetteTestStatistics() {
        testStatistics.printToConsole(courgetteProperties);
    }

    public void printCourgetteTestFailures() {
        CourgetteTestFailure.printTestFailures(getFailures(), courgetteProperties.isFeatureRunLevel());
    }

    private boolean runFeature(CourgetteRunnerInfo runnerInfo, Map<String, List<String>> args) {
        try {
            processFeatureStart();
            CourgetteRun run = new CourgetteFeatureRunner(runnerInfo, args, courgetteProperties, courgettePluginService).run();
            runs.add(run);
            return run.isSuccessful();
        } catch (Throwable throwable) {
            printExceptionStackTrace(throwable);
            return false;
        }
    }

    private boolean rerunFeature(CourgetteRunnerInfo runnerInfo, Map<String, List<String>> args, CourgetteRunResult rerunResult) {
        int rerunAttempts = courgetteProperties.getCourgetteOptions().rerunAttempts();

        rerunAttempts = Math.max(rerunAttempts, 1);

        while (rerunAttempts-- > 0) {
            runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_RERUN, null, rerunResult));
            args.put("retry", new ArrayList<>());
            if (runFeature(runnerInfo, args)) {
                return true;
            }
        }
        return false;
    }

    private int optimizedThreadCount() {
        return requiredThreadCount() > runnerInfoList.size()
                ? runnerInfoList.size()
                : Math.max(requiredThreadCount(), 1);
    }

    private int requiredThreadCount() {
        if (courgetteProperties.isMobileDeviceAllocationPluginEnabled()) {
            int mobileDeviceThreads = courgetteProperties.getMaxThreadsFromMobileDevices(runnerInfoList.get(0).getDeviceType());
            return mobileDeviceThreads > courgetteProperties.getMaxThreads()
                    ? courgetteProperties.getMaxThreads() : mobileDeviceThreads;
        } else {
            return courgetteProperties.getMaxThreads();
        }
    }

    private List<io.cucumber.core.gherkin.Feature> extractRunnerInfoFeatures() {
        return runnerInfoList.stream().map(CourgetteRunnerInfo::getFeature).collect(Collectors.toList());
    }

    private CourgetteRuntimePublisher createRuntimePublisher(CourgetteProperties courgetteProperties, List<io.cucumber.core.gherkin.Feature> features) {
        final Set<CourgettePublisher> publishers = new HashSet<>();
        publishers.add(new SlackPublisher(courgetteProperties));
        publishers.add(new ReportPortalPublisher(courgetteProperties, features));
        return new CourgetteRuntimePublisher(publishers);
    }

    private CourgettePluginService createCourgettePluginService() {
        final CourgetteMobileDeviceAllocatorService mobileDeviceAllocatorService =
                new CourgetteMobileDeviceAllocatorService(courgetteProperties.getCourgetteOptions().mobileDevice());

        return new CourgettePluginService(mobileDeviceAllocatorService);
    }

    private synchronized void addResultAndPublish(CourgetteRunnerInfo courgetteRunnerInfo, CourgetteRunResult
            courgetteRunResult) {
        runResults.add(courgetteRunResult);

        switch (courgetteRunResult.getStatus()) {
            case PASSED:
                runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_PASSED, courgetteRunnerInfo, courgetteRunResult));
                break;
            case PASSED_AFTER_RERUN:
                runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_PASSED_AFTER_RERUN, courgetteRunnerInfo, courgetteRunResult));
                break;
            case FAILED:
            case FAILED_AFTER_RERUN:
                runtimePublisher.publish(createEventHolder(CourgetteEvent.TEST_FAILED, courgetteRunnerInfo, courgetteRunResult));
                break;
        }
    }

    private CourgetteEventHolder createEventHolder(CourgetteEvent courgetteEvent) {
        return new CourgetteEventHolder(courgetteEvent, courgetteProperties);
    }

    private CourgetteEventHolder createEventHolder(CourgetteEvent courgetteEvent, CourgetteRunnerInfo courgetteRunnerInfo, CourgetteRunResult courgetteRunResult) {
        return new CourgetteEventHolder(courgetteEvent, courgetteProperties, courgetteRunnerInfo, courgetteRunResult);
    }

    private CourgetteEventHolder createTestRunSummaryEventHolder() {
        return new CourgetteEventHolder(CourgetteEvent.TEST_RUN_SUMMARY, courgetteProperties, testStatistics);
    }

    private CourgetteReporter createCourgetteReporter() {
        return new CourgetteReporter(runnerInfoList.stream().map(CourgetteRunnerInfo::getCourgetteReportOptions).collect(Collectors.toList()), courgetteProperties);
    }

    private void processFeatureStart() {
        try {
            int threadDelay = courgetteProperties.threadDelay();
            if (threadDelay > 0) {
                Thread.sleep(threadDelay);
            }
        } catch (InterruptedException ignored) {
        }
    }

    private String evaluateRerunFeatureUri(String rerun, String featureUri) {
        return courgetteProperties.isFeatureRunLevel() ? featureUri : (rerun != null && !rerun.trim().isEmpty()) ? rerun : featureUri;
    }

    private List<CourgetteRunnerInfo> conditionallySort(List<CourgetteRunnerInfo> runnerInfoList, CourgetteProperties courgetteProperties) {
        if (courgetteProperties.isMobileDeviceAllocationPluginEnabled()
                && courgetteProperties.isMultipleMobileDeviceTypes()) {

            long simulatorRuns = runnerInfoList.stream().filter(run -> run.getDeviceType().equals(DeviceType.SIMULATOR)).count();
            long realDeviceRuns = runnerInfoList.stream().filter(run -> run.getDeviceType().equals(DeviceType.REAL_DEVICE)).count();

            if (simulatorRuns >= realDeviceRuns) {
                runnerInfoList.sort(Comparator.comparing(devices -> devices.getDeviceType().equals(DeviceType.REAL_DEVICE)));
            } else {
                runnerInfoList.sort(Comparator.comparing(devices -> devices.getDeviceType().equals(DeviceType.SIMULATOR)));
            }
        }
        return runnerInfoList;
    }
}