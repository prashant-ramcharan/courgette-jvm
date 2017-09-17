package courgette.runtime;

public class CourgetteRunResult {
    private String featureName;
    private Integer lineNumber;
    private Status status;

    public CourgetteRunResult(String description, Integer lineNumber, Status status) {
        this.featureName = description;
        this.lineNumber = lineNumber;
        this.status = status;
    }

    public String getFeatureName() {
        return featureName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        PASSED("Passed"),
        FAILED("Failed"),
        PASSED_AFTER_RERUN("Passed after Re-run");

        private String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}