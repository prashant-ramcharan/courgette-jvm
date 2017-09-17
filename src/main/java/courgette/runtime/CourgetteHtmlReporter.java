package courgette.runtime;

import courgette.api.CourgetteRunLevel;
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
    private final String CSS_ASSETS = "/report/css/bootstrap.min.css,/report/css/core.min.css,/report/css/dataTables.bootstrap4.css";
    private final String JS_ASSETS = "/report/js/bootstrap.min.js,/report/js/core.min.js,/report/js/dataTables.bootstrap4.js,/report/js/jquery.dataTables.js,/report/js/jquery.easing.min.js,/report/js/jquery.min.js,/report/js/popper.min.js";

    private final String TARGET_DIR = "target";
    private final String REPORT_DIR = TARGET_DIR + "/courgette-report";
    private final String CSS_DIR = REPORT_DIR + "/css";
    private final String JS_DIR = REPORT_DIR + "/js";

    private final CourgetteProperties courgetteProperties;
    private final List<CourgetteRunResult> courgetteRunResults;

    public CourgetteHtmlReporter(CourgetteProperties courgetteProperties, List<CourgetteRunResult> courgetteRunResults) {
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunResults = courgetteRunResults;
    }

    public void create() {
        createReportDirectories();
        generateHtmlReport();
        copyReportAssets();
    }

    private void generateHtmlReport() {
        final long elapsedMill = (Instant.now().minus(courgetteProperties.getSessionStartTime().toEpochMilli(), ChronoUnit.MILLIS)).toEpochMilli();

        String duration = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedMill),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMill) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMill)));

        final String featureScenarioLabel = courgetteProperties.getCourgetteOptions().runLevel() == CourgetteRunLevel.FEATURE ? "Features" : "Scenarios";

        final int total = courgetteRunResults.size();
        final Long passed = courgetteRunResults.stream().filter(t -> t.getStatus() == CourgetteRunResult.Status.PASSED).count();
        final Long failed = courgetteRunResults.stream().filter(t -> t.getStatus() == CourgetteRunResult.Status.FAILED).count();
        final Long passedAfterRerun = courgetteRunResults.stream().filter(t -> t.getStatus() == CourgetteRunResult.Status.PASSED_AFTER_RERUN).count();
        final Long rerun = courgetteProperties.getCourgetteOptions().rerunFailedScenarios() ? (failed + passedAfterRerun) : 0;

        StringBuilder indexHtmlBuilder = new StringBuilder();

        final InputStream in = getClass().getResourceAsStream(INDEX_HTML);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        reader.lines().forEach(line -> indexHtmlBuilder.append(line).append("\n"));

        String formattedIndexHtml = indexHtmlBuilder.toString();

        formattedIndexHtml = formattedIndexHtml.replaceAll("id:duration", duration);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:label", featureScenarioLabel);
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:total", String.valueOf(total));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:passed", String.valueOf(passed.intValue()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:failed", String.valueOf(failed.intValue()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:rerun", String.valueOf(rerun.intValue()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:after", String.valueOf(passedAfterRerun.intValue()));
        formattedIndexHtml = formattedIndexHtml.replaceAll("id:namelabel", featureScenarioLabel.substring(0, featureScenarioLabel.length() - 1));

        final String resultTableFormat =
                "                        <tr>\n" +
                        "                          <td>%s</td>\n" +
                        "                          <td>%s</td>\n" +
                        "                          <td>%s</td>\n" +
                        "                        </tr>";

        final StringBuilder tableData = new StringBuilder();

        courgetteRunResults.forEach(runResult -> tableData
                .append(String.format(resultTableFormat, runResult.getFeatureName(), runResult.getLineNumber(), runResult.getStatus().getDescription()))
                .append("\n"));

        formattedIndexHtml = formattedIndexHtml.replace("id:results", tableData);

        FileUtils.writeFile(REPORT_DIR + "/index.html", formattedIndexHtml);
    }

    private void copyReportAssets() {
        Arrays.stream(CSS_ASSETS.split(",")).forEach(cssAsset -> {
            InputStream resource = getClass().getResourceAsStream(cssAsset);
            if (resource != null) {
                FileUtils.readAndWriteFile(resource, CSS_DIR + "/" + cssAsset.substring(cssAsset.lastIndexOf("/") + 1));
            }
        });

        Arrays.stream(JS_ASSETS.split(",")).forEach(jsAsset -> {
            InputStream resource = getClass().getResourceAsStream(jsAsset);
            if (resource != null) {
                FileUtils.readAndWriteFile(resource, JS_DIR + "/" + jsAsset.substring(jsAsset.lastIndexOf("/") + 1));
            }
        });
    }

    private void createReportDirectories() {
        final File targetDir = new File(TARGET_DIR);

        if (!targetDir.exists()) {
            if (!targetDir.mkdir()) {
                throw new CourgetteException("Unable to create the 'target' directory");
            }
        }

        final File reportDir = new File(REPORT_DIR);

        if (!reportDir.exists()) {
            if (!reportDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../courgette-report' directory");
            }
        }

        final File cssDir = new File(CSS_DIR);

        if (!cssDir.exists()) {
            if (!cssDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../css' directory");
            }
        }

        final File jsDir = new File(JS_DIR);

        if (!jsDir.exists()) {
            if (!jsDir.mkdir()) {
                throw new CourgetteException("Unable to create the '../js' directory");
            }
        }
    }
}