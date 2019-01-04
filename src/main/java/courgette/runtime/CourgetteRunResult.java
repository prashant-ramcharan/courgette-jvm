package courgette.runtime;

public class CourgetteRunResult {
    private String featureUri;
    private Status status;

    public CourgetteRunResult(String featureUri, Status status) {
        this.featureUri = featureUri;
        this.status = status;
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