package courgette.runtime;

import courgette.runtime.utils.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CucumberJsonReporterTest {
    @Test
    public void mergesJsonReportArrays() throws Exception {
        File sourceOne = File.createTempFile("courgette-source-one", ".json");
        File sourceTwo = File.createTempFile("courgette-source-two", ".json");
        File target = File.createTempFile("courgette-target", ".json");
        FileUtils.writeFile(sourceOne.getPath(), "[{\"name\":\"one\"}]");
        FileUtils.writeFile(sourceTwo.getPath(), "[{\"name\":\"two\"}]");

        CucumberJsonReporter reporter = new CucumberJsonReporter(target.getPath(), 2);
        reporter.readAndWriteReport(sourceOne.getPath());
        reporter.readAndWriteReport(sourceTwo.getPath());

        assertFalse(reporter.hasErrors());
        assertEquals("[{\"name\":\"one\"},{\"name\":\"two\"}]", FileUtils.readFile(target.getPath(), false));
    }

    @Test
    public void recordsErrorsForMissingSourceReports() throws Exception {
        File target = File.createTempFile("courgette-target", ".json");

        CucumberJsonReporter reporter = new CucumberJsonReporter(target.getPath(), 1);
        reporter.readAndWriteReport("/missing/cucumber.json");

        assertTrue(reporter.hasErrors());
        assertTrue(reporter.getErrors().values().iterator().next().contains("does not exist"));
    }
}
