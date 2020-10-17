package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import courgette.runtime.report.builder.HtmlReportBuilder;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CourgetteHtmlReporter {
    private final String INDEX_HTML = "/report/index.html";
    private final String targetDir;
    private final String reportDir;
    private final String reportTitle;

    private final CourgetteProperties courgetteProperties;
    private final List<CourgetteRunResult> courgetteRunResults;
    private final List<Feature> reportFeatures;
    private final String cucumberReportUrl;

    CourgetteHtmlReporter(CourgetteProperties courgetteProperties,
                          List<CourgetteRunResult> courgetteRunResults,
                          List<Feature> reportFeatures,
                          String cucumberReportUrl) {

        this.targetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();
        this.reportTitle = courgetteProperties.getCourgetteOptions().reportTitle();
        this.reportDir = targetDir + "/courgette-report";
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunResults = courgetteRunResults;
        this.reportFeatures = reportFeatures;
        this.cucumberReportUrl = cucumberReportUrl;
    }

    public void create() {
        createReportDirectories();
        generateHtmlReport();
    }

    private void generateHtmlReport() {
        final long elapsedMill = (Instant.now().minus(courgetteProperties.getSessionStartTime().toEpochMilli(), ChronoUnit.MILLIS)).toEpochMilli();

        String duration = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedMill),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMill)));

        boolean isFeatureRunLevel = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE;

        final String featureScenarioLabel = isFeatureRunLevel ? "Features" : "Scenarios";

        int total, passed, failed, rerun;

        if (isFeatureRunLevel) {
            total = reportFeatures.size();
            passed = (int) reportFeatures.stream().filter(Feature::passed).count();
        } else {
            List<Scenario> scenarioList = reportFeatures.stream().flatMap(f -> f.getScenarios().stream()).collect(Collectors.toList());
            total = scenarioList.size();
            passed = (int) scenarioList.stream().filter(Scenario::passed).count();
        }

        failed = total - passed;
        rerun = courgetteProperties.getCourgetteOptions().rerunFailedScenarios() ? (int) courgetteRunResults.stream().filter(result -> result.getStatus().equals(CourgetteRunResult.Status.RERUN)).count() : 0;

        StringBuilder indexHtmlBuilder = new StringBuilder();

        final InputStream in = getClass().getResourceAsStream(INDEX_HTML);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        reader.lines().forEach(line -> indexHtmlBuilder.append(line).append("\n"));

        String formattedIndexHtml = indexHtmlBuilder.toString();

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:reportTitle", reportTitle);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:label", featureScenarioLabel);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:total", String.valueOf(total));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:passed", String.valueOf(passed));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:failed", String.valueOf(failed));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:rerun", String.valueOf(rerun));

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:timestamp", Instant.now().toString());
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:duration", duration);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:threads", String.valueOf(courgetteProperties.getMaxThreads()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:runlevel", courgetteProperties.getCourgetteOptions().runLevel().toString());
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:cucumber_report", cucumberReportUrl);

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:os_name", System.getProperty("os.name"));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:os_arch", System.getProperty("os.arch"));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:java_version", System.getProperty("java.version"));

        String cucumberTags = System.getProperty("cucumber.tags", "Not provided");

        if (cucumberTags.equals("Not provided")) {
            String[] tags = courgetteProperties.getCourgetteOptions().cucumberOptions().tags();
            if (tags.length > 0) {
                cucumberTags = Arrays.asList(tags).toString().replace("[", "").replace("]", "");
            }
        }

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:tags", cucumberTags);

        String featureDir = Arrays.asList(courgetteProperties.getCourgetteOptions().cucumberOptions().features()).toString().replace("[", "").replace("]", "");
        String cucumberFeatures = System.getProperty("cucumber.features", featureDir);

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:features", cucumberFeatures);

        final HtmlReportBuilder htmlReportBuilder = HtmlReportBuilder.create(reportFeatures, courgetteRunResults, courgetteProperties);

        final String results = htmlReportBuilder.getHtmlTableFeatureRows();

        formattedIndexHtml = formattedIndexHtml.replace("id:results", results);
        formattedIndexHtml = formattedIndexHtml.replace("id:modals", htmlReportBuilder.getHtmlModals());

        FileUtils.writeFile(reportDir + "/index.html", formattedIndexHtml);
    }

    private void createReportDirectories() {
        final File targetDir = new File(this.targetDir);

        if (!targetDir.exists()) {
            if (!targetDir.mkdir()) {
                throw new CourgetteException(String.format("Unable to create the '%s' directory", targetDir));
            }
        }

        final File reportDir = new File(this.reportDir);

        if (!reportDir.exists()) {
            if (!reportDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../courgette-report' directory");
            }
        }
    }
}