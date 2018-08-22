package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import courgette.runtime.report.JsonReportParser;
import courgette.runtime.report.builder.HtmlReportBuilder;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Step;
import courgette.runtime.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CourgetteHtmlReporter {
    private final String INDEX_HTML = "/report/index.html";
    private final String CSS_ASSETS = "/report/css/bootstrap.min.css,/report/css/core.min.css,/report/css/dataTables.bootstrap4.css";
    private final String JS_ASSETS = "/report/js/bootstrap.min.js,/report/js/core.min.js,/report/js/dataTables.bootstrap4.js,/report/js/jquery.dataTables.js,/report/js/jquery.easing.min.js,/report/js/jquery.min.js,/report/js/popper.min.js,/report/js/mdb.min.js,/report/js/Chart.min.js";

    private final String targetDir;
    private final String reportDir;
    private final String imagesDir;
    private final String cssDir;
    private final String jsDir;

    private final CourgetteProperties courgetteProperties;
    private List<CourgetteRunResult> courgetteRunResults;
    private String cucumberJsonReport;

    public CourgetteHtmlReporter(CourgetteProperties courgetteProperties, List<CourgetteRunResult> courgetteRunResults, String cucumberJsonReport) {
        this.targetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();
        this.reportDir = targetDir + "/courgette-report";
        this.imagesDir = reportDir + "/images";
        this.cssDir = reportDir + "/css";
        this.jsDir = reportDir + "/js";

        this.courgetteProperties = courgetteProperties;
        this.courgetteRunResults = courgetteRunResults;
        this.cucumberJsonReport = cucumberJsonReport;
    }

    public void create() {
        createReportDirectories();
        generateHtmlReport();
        copyReportAssets();
    }

    private void generateHtmlReport() {
        final List<Feature> features;

        try {
            features = new ArrayList<>(JsonReportParser.create(new File(cucumberJsonReport)).getReportFeatures());
        } catch (CourgetteException e) {
            System.err.println("Unable to create the Courgette Html Report -> " + e.getMessage());
            return;
        }

        final long elapsedMill = (Instant.now().minus(courgetteProperties.getSessionStartTime().toEpochMilli(), ChronoUnit.MILLIS)).toEpochMilli();

        String duration = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedMill),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMill)));

        final String featureScenarioLabel = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE ? "Features" : "Scenarios";
        final boolean isStrict = courgetteProperties.getCourgetteOptions().cucumberOptions().strict();

        final int total = features.size();
        final int passed = (int) features.stream().filter(feature -> feature.passed(isStrict)).count();
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

        final Consumer<Embedding> saveImage = (embedding) -> {
            if (embedding.getMimeType().startsWith("image")) {
                final String imageName = imagesDir + "/" + embedding.getCourgetteEmbeddingId();
                final String imageFormat = embedding.getMimeType().split("/")[1];
                FileUtils.writeImageFile(imageName, imageFormat, embedding.getData());
            }
        };

        features.forEach(feature ->
                feature.getScenarios().forEach(scenario -> {
                    scenario.getBefore().forEach(before -> before.getEmbeddings().forEach(saveImage));
                    scenario.getAfter().forEach(after -> after.getEmbeddings().forEach(saveImage));

                    List<Step> scenarioSteps = scenario.getSteps();

                    scenarioSteps.forEach(step -> step.getEmbeddings().forEach(saveImage));
                    scenarioSteps.forEach(step -> step.getBefore().forEach(before -> before.getEmbeddings().forEach(saveImage)));
                    scenarioSteps.forEach(step -> step.getAfter().forEach(after -> after.getEmbeddings().forEach(saveImage)));
                }));

        final HtmlReportBuilder htmlReportBuilder = HtmlReportBuilder.create(features, isStrict);

        final String results = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE ?
                htmlReportBuilder.getHtmlTableFeatureRows() :
                htmlReportBuilder.getHtmlTableScenarioRows();

        formattedIndexHtml = formattedIndexHtml.replace("id:results", results);
        formattedIndexHtml = formattedIndexHtml.replace("id:modals", htmlReportBuilder.getHtmlModals());

        FileUtils.writeFile(reportDir + "/index.html", formattedIndexHtml);
    }

    private void copyReportAssets() {
        Arrays.stream(CSS_ASSETS.split(",")).forEach(cssAsset -> {
            InputStream resource = getClass().getResourceAsStream(cssAsset);
            if (resource != null) {
                FileUtils.readAndWriteFile(resource, cssDir + "/" + cssAsset.substring(cssAsset.lastIndexOf("/") + 1));
            }
        });

        Arrays.stream(JS_ASSETS.split(",")).forEach(jsAsset -> {
            InputStream resource = getClass().getResourceAsStream(jsAsset);
            if (resource != null) {
                FileUtils.readAndWriteFile(resource, jsDir + "/" + jsAsset.substring(jsAsset.lastIndexOf("/") + 1));
            }
        });
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

        final File cssDir = new File(this.cssDir);

        if (!cssDir.exists()) {
            if (!cssDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../css' directory");
            }
        }

        final File jsDir = new File(this.jsDir);

        if (!jsDir.exists()) {
            if (!jsDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../js' directory");
            }
        }

        final File imagesDir = new File(this.imagesDir);

        if (!imagesDir.exists()) {
            if (!imagesDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../images' directory");
            }
        }
    }
}