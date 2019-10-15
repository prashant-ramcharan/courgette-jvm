package courgette.runtime;

import io.cucumber.core.feature.CucumberFeature;

public class CourgetteRunResult {
    private CucumberFeature cucumberFeature;
    private Integer lineId;
    private String featureUri;
    private Status status;

    public CourgetteRunResult(CucumberFeature cucumberFeature, Integer lineId, String featureUri, Status status) {
        this.cucumberFeature = cucumberFeature;
        this.lineId = lineId;
        this.featureUri = featureUri;
        this.status = status;
    }

    public CucumberFeature getCucumberFeature() {
        return cucumberFeature;
    }

    public Integer getLineId() {
        return lineId;
    }

    public String getFeatureUri() {
        return featureUri;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        PASSED("Passed"),
        FAILED("Failed"),
        RERUN("Re-run"),
        PASSED_AFTER_RERUN("Passed after Rerun");

        private String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}