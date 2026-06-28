package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CucumberOptions;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.report.model.Step;
import courgette.runtime.utils.FileUtils;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CourgetteHtmlReporterTest {
    @Test
    public void displaysPublishedCucumberReportUrl() throws Exception {
        File reportTargetDir = Files.createTempDirectory("courgette-html-report").toFile();
        String reportUrl = "https://reports.cucumber.io/reports/report-id";

        createReport(reportTargetDir, reportUrl);

        String html = FileUtils.readFile(new File(reportTargetDir, "courgette-report/index.html").getPath(), false);
        assertTrue(html.contains("<a target=\"_blank\" href=\"" + reportUrl + "\">View published report</a>"));
    }

    @Test
    public void displaysNotPublishedWhenCucumberReportUrlIsNotAvailable() throws Exception {
        File reportTargetDir = Files.createTempDirectory("courgette-html-report").toFile();

        createReport(reportTargetDir, "#");

        String html = FileUtils.readFile(new File(reportTargetDir, "courgette-report/index.html").getPath(), false);
        assertTrue(html.contains("Not published"));
    }

    private void createReport(File reportTargetDir, String cucumberReportUrl) throws Exception {
        CourgetteProperties properties = new CourgetteProperties(new CourgetteRunOptions(HtmlReportRunner.class), "session", 1) {
            @Override
            public CourgetteOptions getCourgetteOptions() {
                return new CourgetteRunOptions(HtmlReportRunner.class) {
                    @Override
                    public String reportTargetDir() {
                        return reportTargetDir.getPath();
                    }
                };
            }
        };

        List<Feature> features = Collections.singletonList(feature());
        List<CourgetteRunResult> runResults = Collections.singletonList(
                new CourgetteRunResult(null, 4, "src/test/resources/features/integration/local-web-app.feature:4", CourgetteRunResult.Status.PASSED));

        CourgetteHtmlReporter reporter = new CourgetteHtmlReporter(properties, runResults, features, cucumberReportUrl);
        reporter.create(CourgetteTestStatistics.current());
    }

    private Feature feature() {
        String featureUri = "src/test/resources/features/integration/local-web-app.feature";
        Scenario scenario = new Scenario(
                featureUri,
                "2026-06-28T14:35:38.334610Z",
                "A passing scenario",
                "Scenario",
                4,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(step()),
                Collections.emptyList());

        return new Feature("Local web app", featureUri, Collections.singletonList(scenario));
    }

    private Step step() {
        return new Step(
                "local web app page 1 is displayed",
                "Then ",
                new Result("PASSED", 1000000, null),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    @CourgetteOptions(
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression"))
    private static class HtmlReportRunner {
    }
}
