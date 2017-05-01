package courgette.runtime;

import courgette.api.cli.Main;
import courgette.runtime.utils.FileUtils;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.model.CucumberFeature;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CourgetteRunner {
    private final List<Callable<Boolean>> features = new ArrayList<>();
    private final Map<String, CopyOnWriteArrayList<String>> reruns = new LinkedHashMap<>();
    private final Map<String, CopyOnWriteArrayList<String>> reports = new LinkedHashMap<>();

    private final Integer totalFeatures;
    private final AtomicInteger featuresPassed = new AtomicInteger(0);
    private final AtomicInteger featuresFailed = new AtomicInteger(0);
    private final AtomicInteger featuresRerun = new AtomicInteger(0);
    private final AtomicInteger featuresRerunPassed = new AtomicInteger(0);

    private final List<FeatureRunner> featureRunners;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRuntimeOptions defaultRuntimeOptions;
    private final Boolean rerunFailedScenarios;

    private final StringBuilder executionLog = new StringBuilder();

    public CourgetteRunner(List<FeatureRunner> featureRunners, CourgetteProperties courgetteProperties) {
        this.featureRunners = featureRunners;
        this.courgetteProperties = courgetteProperties;
        this.rerunFailedScenarios = courgetteProperties.getCourgetteOptions().rerunFailedScenarios();
        this.totalFeatures = featureRunners.size();
        this.defaultRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
    }

    public void run() {
        final ExecutorService executor = Executors.newFixedThreadPool(courgetteProperties.getMaxThreads());

        do {
            List<FeatureRunner> featureRunners = new ArrayList<>(this.featureRunners);

            featureRunners.forEach(featureRunner -> {
                CucumberFeature cucumberFeature = new CourgetteFeature(featureRunner).getCucumberFeature();
                final String featureName = cucumberFeature.getGherkinFeature().getName();

                final CourgetteRuntimeOptions runtimeOptions = new CourgetteRuntimeOptions(courgetteProperties, cucumberFeature);

                final String[] cucumberArgs = runtimeOptions.getRuntimeOptions();

                this.featureRunners.remove(featureRunner);

                features.add(() -> {
                    try {
                        Boolean isPassed = runFeature("Running feature", featureName, cucumberArgs);

                        if (isPassed) {
                            featuresPassed.incrementAndGet();
                            return Boolean.TRUE;
                        }

                        featuresFailed.incrementAndGet();

                        String rerunFile = runtimeOptions.getRerunFile();
                        String rerunFeature = FileUtils.readFile(rerunFile, Boolean.FALSE);

                        if (rerunFailedScenarios && rerunFeature != null) {
                            String[] rerunCucumberArgs = runtimeOptions.getRerunRuntimeOptions(rerunFeature);

                            isPassed = runFeature("Re-running failed scenario", rerunFeature, rerunCucumberArgs);

                            featuresRerun.incrementAndGet();

                            if (isPassed) {
                                featuresFailed.decrementAndGet();
                                featuresPassed.incrementAndGet();
                                featuresRerunPassed.incrementAndGet();
                                return Boolean.TRUE;
                            }
                        }

                        if (rerunFeature != null) {
                            final CopyOnWriteArrayList<String> rerun = new CopyOnWriteArrayList<>();
                            rerun.add(rerunFeature);
                            reruns.put(featureName, rerun);
                        } else {
                            System.err.println("Something went wrong. Unable to read from re-run file.");
                        }
                    } finally {
                        runtimeOptions.getReportJsFiles().forEach(reportFile -> {
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
            });
        } while (this.featureRunners.size() > 0);

        try {
            executor.invokeAll(features);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }

    public void createReport() {
        if (reports.values().stream().allMatch(t -> t.contains(null) || t.contains("null"))) {
            return;
        }

        final List<String> reportFiles = defaultRuntimeOptions.getReportJsFiles();

        reportFiles.forEach(reportFile -> {
            CourgetteReporter reporter = new CourgetteReporter(reportFile, reports);
            reporter.createReport();
        });
    }

    public void createRerunFile() {
        final Map<String, CopyOnWriteArrayList<String>> sortedReruns = new LinkedHashMap<>();

        reruns.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sortedReruns.put(x.getKey(), x.getValue()));

        final List<String> rerun = new ArrayList<>();
        sortedReruns.values().forEach(rerunFeature -> {
            try {
                rerun.add(rerunFeature.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        final String rerunFile = defaultRuntimeOptions.getCucumberRerunFile();

        if (rerunFile != null) {
            FileUtils.writeFile(rerunFile, rerun);
        }
    }

    public void createExecutionReport() {
        final CourgetteExecutionReporter executionReporter = new CourgetteExecutionReporter(executionLog);

        executionReporter.createReport(
                totalFeatures,
                featuresPassed.get(),
                featuresFailed.get(),
                featuresRerun.get(),
                featuresRerunPassed.get(),
                courgetteProperties.getSessionStartTime());
    }

    public Boolean allFeaturesPassed() {
        return featuresFailed.get() == 0;
    }

    private Boolean runFeature(String msg, String feature, String[] args) throws IOException {
        try {
            final Boolean result = (0x0 == Main.run(args));

            executionLog.append(String.format("\n[THREAD-%s] %s: '%s' -> %s",
                    Thread.currentThread().getId(),
                    msg,
                    feature,
                    result ? "PASSED" : "FAILED"));

            return result;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Boolean.FALSE;
        }
    }

    private String prettyJson(String json) {
        final Object jsonObject = new Gson().fromJson(json, Object.class);
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }
}