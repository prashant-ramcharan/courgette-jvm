package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import courgette.runtime.report.builder.HtmlReportBuilder;
import courgette.runtime.report.model.Feature;
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

public class CourgetteHtmlReporter {
    private final String INDEX_HTML = "/report/index.html";
    private final String targetDir;
    private final String reportDir;

    private final CourgetteProperties courgetteProperties;
    private final List<CourgetteRunResult> courgetteRunResults;
    private final List<Feature> reportFeatures;

    public CourgetteHtmlReporter(CourgetteProperties courgetteProperties, List<CourgetteRunResult> courgetteRunResults, List<Feature> reportFeatures) {
        this.targetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();
        this.reportDir = targetDir + "/courgette-report";
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunResults = courgetteRunResults;
        this.reportFeatures = reportFeatures;
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

        final String featureScenarioLabel = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE ? "Features" : "Scenarios";
        final boolean isStrict = courgetteProperties.getCourgetteOptions().cucumberOptions().strict();

        final int total = reportFeatures.size();
        final int passed = (int) reportFeatures.stream().filter(feature -> feature.passed(isStrict)).count();
        final int failed = total - passed;
        final int rerun = courgetteProperties.getCourgetteOptions().rerunFailedScenarios() ? (int) courgetteRunResults.stream().filter(result -> result.getStatus().equals(CourgetteRunResult.Status.RERUN)).count() : 0;

        StringBuilder indexHtmlBuilder = new StringBuilder();

        final InputStream in = getClass().getResourceAsStream(INDEX_HTML);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        reader.lines().forEach(line -> indexHtmlBuilder.append(line).append("\n"));

        String formattedIndexHtml = indexHtmlBuilder.toString();

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:label", featureScenarioLabel);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:total", String.valueOf(total));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:passed", String.valueOf(passed));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:failed", String.valueOf(failed));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:rerun", String.valueOf(rerun));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:namelabel", featureScenarioLabel.substring(0, featureScenarioLabel.length() - 1));

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:timestamp", Instant.now().toString());
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:duration", duration);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:threads", String.valueOf(courgetteProperties.getMaxThreads()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:runlevel", courgetteProperties.getCourgetteOptions().runLevel().toString());
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:retry", String.valueOf(courgetteProperties.getCourgetteOptions().rerunFailedScenarios() ? "true" : "false"));

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

        final HtmlReportBuilder htmlReportBuilder = HtmlReportBuilder.create(reportFeatures, courgetteRunResults, isStrict);

        final String results = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE ?
                htmlReportBuilder.getHtmlTableFeatureRows() :
                htmlReportBuilder.getHtmlTableScenarioRows();

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