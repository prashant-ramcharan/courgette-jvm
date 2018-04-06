package courgette.runtime;

public class CourgetteRunResult {
    private Status status;

    public CourgetteRunResult(Status status) {
        this.status = status;
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