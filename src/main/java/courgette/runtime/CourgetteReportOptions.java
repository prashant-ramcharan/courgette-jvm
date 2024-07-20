package courgette.runtime;

import courgette.runtime.utils.FileUtils;
import io.cucumber.core.gherkin.Feature;

import java.util.Arrays;
import java.util.Optional;

public class CourgetteReportOptions {
    private final Feature feature;
    private final String jsonFile;
    private final String ndjsonFile;
    private Optional<String> xmlFile = Optional.empty();
    private Optional<String> rerunFile = Optional.empty();

    public CourgetteReportOptions(Feature feature, Integer lineId, CourgetteProperties courgetteProperties) {
        this.feature = feature;

        final String fileId = FileUtils.tempDirectory() + createFeatureIdWithLine(lineId) + courgetteProperties.getSessionId();

        jsonFile = fileId + ".json";
        ndjsonFile = fileId + ".ndjson";

        if (courgetteProperties.isCucumberXmlReportPluginEnabled() || courgetteProperties.isReportPortalPluginEnabled()) {
            xmlFile = Optional.of(fileId + ".xml");
        }

        if (courgetteProperties.getCourgetteOptions().rerunFailedScenarios()) {
            rerunFile = Optional.of(fileId + ".txt");
        }
    }

    public Feature getFeature() {
        return feature;
    }

    public String getJsonFile() {
        return jsonFile;
    }

    public String getNdJsonFile() {
        return ndjsonFile;
    }

    public Optional<String> getXmlFile() {
        return xmlFile;
    }

    public Optional<String> getRerunFile() {
        return rerunFile;
    }

    public String getFeatureId() {
        return Arrays.stream(feature.getUri().getPath().split("/")).reduce((x, y) -> y)
                .orElse("").replace(".feature", "")
                .toLowerCase();
    }

    private String createFeatureIdWithLine(Integer lineId) {
        return getFeatureId() + "_" + (lineId == null ? "" : (lineId + "_"));
    }
}
