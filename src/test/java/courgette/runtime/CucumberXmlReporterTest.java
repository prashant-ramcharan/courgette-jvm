package courgette.runtime;

import courgette.runtime.utils.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CucumberXmlReporterTest {
    @Test
    public void mergesXmlReportsAndRenamesDuplicateTestCases() throws Exception {
        File sourceOne = File.createTempFile("courgette-source-one", ".xml");
        File sourceTwo = File.createTempFile("courgette-source-two", ".xml");
        File target = File.createTempFile("courgette-target", ".xml");
        FileUtils.writeFile(sourceOne.getPath(), report("1", "1.5"));
        FileUtils.writeFile(sourceTwo.getPath(), report("0", "2.0"));

        CucumberXmlReporter reporter = new CucumberXmlReporter(target.getPath(), 2);
        reporter.readAndWriteReport(sourceOne.getPath(), false, false);
        reporter.readAndWriteReport(sourceTwo.getPath(), false, false);

        String merged = FileUtils.readFile(target.getPath(), false);
        assertFalse(reporter.hasErrors());
        assertTrue(merged.contains("failures=\"1\""));
        assertTrue(merged.contains("tests=\"4\""));
        assertTrue(merged.contains("time=\"3.5\""));
        assertTrue(merged.contains("name=\"scenario\""));
        assertTrue(merged.contains("name=\"scenario 2\""));
    }

    @Test
    public void canMergeClassNameIntoTestCaseName() throws Exception {
        File source = File.createTempFile("courgette-source", ".xml");
        File target = File.createTempFile("courgette-target", ".xml");
        FileUtils.writeFile(source.getPath(), report("0", "1.0"));

        CucumberXmlReporter reporter = new CucumberXmlReporter(target.getPath(), 1);
        reporter.readAndWriteReport(source.getPath(), true, false);

        assertTrue(FileUtils.readFile(target.getPath(), false).contains("name=\"feature: scenario\""));
    }

    @Test
    public void recordsErrorsForMissingXmlReports() throws Exception {
        File target = File.createTempFile("courgette-target", ".xml");

        CucumberXmlReporter reporter = new CucumberXmlReporter(target.getPath(), 1);
        reporter.readAndWriteReport("/missing/cucumber.xml", false, false);

        assertTrue(reporter.hasErrors());
    }

    private String report(String failures, String time) {
        return "<testsuite failures=\"" + failures + "\" skipped=\"1\" tests=\"2\" time=\"" + time + "\">"
                + "<testcase classname=\"feature\" name=\"scenario\" time=\"0.1\"/>"
                + "<testcase classname=\"feature\" name=\"scenario\" time=\"0.2\"/>"
                + "</testsuite>";
    }
}
