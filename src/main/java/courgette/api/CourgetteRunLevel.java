package courgette.api;

public enum CourgetteRunLevel {
    FEATURE,
    SCENARIO;

    @Override
    public String toString() {
        switch (this) {
            case FEATURE:
                return "Feature";

            default:
                return "Scenario";
        }
    }
}