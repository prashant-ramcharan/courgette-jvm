package courgette.runtime;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import courgette.runtime.report.builder.HtmlReportBuilder;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Feature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CourgetteHtmlReporter {
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

    public void create(CourgetteTestStatistics testStatistics) throws IOException {
        createReportDirectories();
        generateHtmlReport(testStatistics);
    }

    private void generateHtmlReport(CourgetteTestStatistics testStatistics) throws IOException {
        String featureScenarioLabel = courgetteProperties.isFeatureRunLevel() ? "Features" : "Scenarios";

        String cucumberTags = System.getProperty("cucumber.tags", "Not provided");
        if (cucumberTags.equals("Not provided")) {
            String[] tags = courgetteProperties.getCourgetteOptions().cucumberOptions().tags();
            if (tags.length > 0) {
                cucumberTags = Arrays.asList(tags).toString().replace("[", "").replace("]", "");
            }
        }

        String featureDir = Arrays.asList(courgetteProperties.getCourgetteOptions().cucumberOptions().features()).toString().replace("[", "").replace("]", "");
        String cucumberFeatures = System.getProperty("cucumber.features", featureDir);

        final HtmlReportBuilder htmlReportBuilder = HtmlReportBuilder.create(reportFeatures, courgetteRunResults, courgetteProperties);

        final List<String> results = htmlReportBuilder.getHtmlTableFeatureRows();

        final List<String> modals = htmlReportBuilder.getHtmlModals();

        final HashMap<String, Object> reportData = new HashMap<>();
        reportData.put("reportTitle", reportTitle);
        reportData.put("label", featureScenarioLabel);
        reportData.put("total", testStatistics.total());
        reportData.put("passed", testStatistics.passed());
        reportData.put("failed", testStatistics.failed());
        reportData.put("rerun", testStatistics.rerun());
        reportData.put("timestamp", Instant.now().toString());
        reportData.put("duration", testStatistics.duration());
        reportData.put("threads", courgetteProperties.getMaxThreads());
        reportData.put("run_level", courgetteProperties.getCourgetteOptions().runLevel().toString());
        reportData.put("cucumber_report", cucumberReportUrl);
        reportData.put("os_name", System.getProperty("os.name"));
        reportData.put("os_arch", System.getProperty("os.arch"));
        reportData.put("java_version", System.getProperty("java.version"));
        reportData.put("tags", cucumberTags);
        reportData.put("features", cucumberFeatures);
        reportData.put("results", results);
        reportData.put("modals", modals);

        File thisFile = new File(reportDir + "/index.html");

        BufferedWriter writer = new BufferedWriter(new FileWriter(thisFile, false));

        final InputStream in = getClass().getResourceAsStream("/report/templates/index.mustache");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        final Mustache report = new DefaultMustacheFactory().compile(reader, "");
        report.execute(writer, reportData);

        createImageScript(writer, reportFeatures);

        writer.close();
    }

    private void createImageScript(Writer writer, List<Feature> reportFeatures) throws IOException {

        final List<Embedding> embeddings = new ArrayList<>();

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getBefore().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getSteps().stream())
                .flatMap(t -> t.getBefore().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getSteps().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getSteps().stream())
                .flatMap(t -> t.getAfter().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        reportFeatures.stream().map(Feature::getScenarios)
                .flatMap(Collection::stream)
                .flatMap(t -> t.getAfter().stream())
                .flatMap(t -> t.getEmbeddings().stream())
                .forEach(embeddings::add);

        final List<Embedding> imageEmbeddings = embeddings.stream().filter(e -> e.getMimeType().startsWith("image")).collect(Collectors.toList());

        writer.write("\n<script>\n");

        for (Embedding embedding : imageEmbeddings) {
            writer.write("document.getElementById('");
            writer.write(embedding.getCourgetteEmbeddingId());
            writer.write("').src='data:image;base64,");
            writer.write(embedding.getData());
            writer.write("'\n\n");
        }

        writer.write("</script>");
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