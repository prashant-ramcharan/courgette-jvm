package courgette.runtime.event;

public enum CourgetteEvent {

    ALL(""),
    TEST_RUN_STARTED("Test Run Started"),
    TEST_RUN_FINISHED("Test Run Finished"),
    TEST_PASSED("Test Passed"),
    TEST_PASSED_AFTER_RERUN("Test Passed"),
    TEST_FAILED("Test Failed"),
    TEST_RERUN("Test Rerun"),
    TEST_RUN_SUMMARY("Test Run Summary");

    private final String description;

    CourgetteEvent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}