package courgette.runtime.report;

import courgette.runtime.report.model.Feature;
import courgette.runtime.utils.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonReportParserTest {
    @Test
    public void parsesFeatureLevelJsonReports() throws Exception {
        File report = File.createTempFile("courgette-report", ".json");
        FileUtils.writeFile(report.getPath(), cucumberJson("features/demo.feature", "first"));

        JsonReportParser parser = new JsonReportParser(report.getPath(), true);
        parser.createFeatures();

        List<Feature> features = parser.getFeatures();
        assertEquals(1, features.size());
        assertEquals("Demo", features.get(0).getName());
        assertEquals(1, features.get(0).getScenarios().size());
        assertTrue(features.get(0).passed());
        assertEquals("@regression", features.get(0).getScenarios().get(0).getTags().get(0).getName());
        assertEquals("output", features.get(0).getScenarios().get(0).getSteps().get(0).getOutput().get(0));
        assertEquals("| cell | ", features.get(0).getScenarios().get(0).getSteps().get(0).getRowData().get(0));
    }

    @Test
    public void combinesScenarioLevelReportsByFeatureUri() throws Exception {
        File report = File.createTempFile("courgette-report", ".json");
        FileUtils.writeFile(report.getPath(), "["
                + featureJson("features/demo.feature", "second", 12, "passed")
                + ","
                + featureJson("features/demo.feature", "first", 5, "failed")
                + "]");

        JsonReportParser parser = new JsonReportParser(report.getPath(), false);
        parser.createFeatures();

        List<Feature> features = parser.getFeatures();
        assertEquals(1, features.size());
        assertEquals(2, features.get(0).getScenarios().size());
        assertEquals("first", features.get(0).getScenarios().get(0).getName());
        assertFalse(features.get(0).passed());
    }

    private String cucumberJson(String uri, String scenarioName) {
        return "[" + featureJson(uri, scenarioName, 5, "passed") + "]";
    }

    private String featureJson(String uri, String scenarioName, int line, String status) {
        return "{"
                + "\"name\":\"Demo\","
                + "\"uri\":\"" + uri + "\","
                + "\"elements\":[{"
                + "\"name\":\"" + scenarioName + "\","
                + "\"keyword\":\"Scenario\","
                + "\"line\":" + line + ","
                + "\"start_timestamp\":\"2026-01-01T00:00:00Z\","
                + "\"tags\":[{\"name\":\"@regression\"}],"
                + "\"before\":[{\"result\":{\"status\":\"passed\",\"duration\":1},\"match\":{\"location\":\"hooks.Before\"}}],"
                + "\"after\":[{\"result\":{\"status\":\"passed\",\"duration\":1},\"match\":{\"location\":\"hooks.After\"}}],"
                + "\"steps\":[{"
                + "\"name\":\"step\","
                + "\"keyword\":\"Given \","
                + "\"result\":{\"status\":\"" + status + "\",\"duration\":2},"
                + "\"match\":{\"location\":\"steps.Step\"},"
                + "\"output\":[\"output\"],"
                + "\"embeddings\":[{\"mime_type\":\"text/plain\",\"data\":\"hello\"}],"
                + "\"rows\":[{\"cells\":[\"cell\"]}]"
                + "}]"
                + "}]"
                + "}";
    }
}
