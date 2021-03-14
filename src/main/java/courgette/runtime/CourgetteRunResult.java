package courgette.runtime;

import io.cucumber.core.gherkin.Feature;

public class CourgetteRunResult {
    private Feature feature;
    private Integer lineId;
    private String featureUri;
    private Status status;

    CourgetteRunResult(Feature feature, Integer lineId, String featureUri, Status status) {
        this.feature = feature;
        this.lineId = lineId;
        this.featureUri = featureUri;
        this.status = status;
    }

    public Feature getFeature() {
        return feature;
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
        PASSED_AFTER_RERUN("Passed after Rerun"),
        FAILED_AFTER_RERUN("Failed after Rerun");

        private String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}