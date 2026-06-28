package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CucumberOptions;
import courgette.runtime.utils.FileUtils;
import io.cucumber.core.gherkin.Feature;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourgetteReporterTest {
    @Test
    public void deletesTemporaryReportFiles() throws Exception {
        Fixture fixture = fixture();
        CourgetteReportOptions reportOptions = new CourgetteReportOptions(fixture.feature, null, fixture.properties);
        FileUtils.writeFile(reportOptions.getJsonFile(), "[]");
        FileUtils.writeFile(reportOptions.getNdJsonFile(), "");
        reportOptions.getXmlFile().ifPresent(file -> FileUtils.writeFile(file, "<testsuite/>"));
        reportOptions.getRerunFile().ifPresent(file -> FileUtils.writeFile(file, ""));

        CourgetteReporter reporter = new CourgetteReporter(Collections.singletonList(reportOptions), fixture.properties);
        reporter.deleteTemporaryReports();

        assertFalse(new File(reportOptions.getJsonFile()).exists());
        assertFalse(new File(reportOptions.getNdJsonFile()).exists());
        assertFalse(new File(reportOptions.getXmlFile().get()).exists());
        assertFalse(new File(reportOptions.getRerunFile().get()).exists());
    }

    @Test
    public void writesReportProcessingErrors() throws Exception {
        File reportTargetDir = Files.createTempDirectory("courgette-report-target").toFile();
        Fixture fixture = fixture(reportTargetDir.getPath());
        CourgetteReportOptions reportOptions = new CourgetteReportOptions(fixture.feature, null, fixture.properties);
        CourgetteReporter reporter = new CourgetteReporter(Collections.singletonList(reportOptions), fixture.properties);

        reporter.createCucumberReports(Collections.singletonList(new File(reportTargetDir, "combined.json").getPath()), false);
        reporter.createErrorReport();

        File errorReport = new File(reportTargetDir, "courgette-report-processing-errors.txt");
        assertTrue(reporter.hasErrors());
        assertTrue(errorReport.exists());
        assertTrue(FileUtils.readFile(errorReport.getPath(), false).contains("Processing errors"));
    }

    private Fixture fixture() {
        return fixture("build");
    }

    private Fixture fixture(String reportTargetDir) {
        CourgetteProperties properties = new CourgetteProperties(new CourgetteRunOptions(ReporterRunner.class), "session", 1) {
            @Override
            public courgette.api.CourgetteOptions getCourgetteOptions() {
                return new CourgetteRunOptions(ReporterRunner.class) {
                    @Override
                    public String reportTargetDir() {
                        return reportTargetDir;
                    }
                };
            }
        };
        Feature feature = new CourgetteLoader(properties).getFeatures().get(0);
        return new Fixture(properties, feature);
    }

    private static class Fixture {
        private final CourgetteProperties properties;
        private final Feature feature;

        private Fixture(CourgetteProperties properties, Feature feature) {
            this.properties = properties;
            this.feature = feature;
        }
    }

    @CourgetteOptions(
            rerunFailedScenarios = true,
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression",
                    plugin = "junit:build/cucumber.xml"))
    private static class ReporterRunner {
    }
}
