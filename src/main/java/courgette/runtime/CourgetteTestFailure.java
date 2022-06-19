package courgette.runtime;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;

import java.util.List;
import java.util.Optional;

public final class CourgetteTestFailure {

    public static void printTestFailures(List<CourgetteRunResult> failures, boolean isFeatureRunLevel) {
        if (!failures.isEmpty()) {
            StringBuilder testFailures = new StringBuilder();
            testFailures.append(String.format("\nThe following %s failed: \n",
                    isFeatureRunLevel ? "features" : "scenarios"));

            failures.forEach(failure -> {
                Feature feature = failure.getFeature();

                String testId = feature.getName().orElse("Test");

                if (failure.getLineId() != null) {
                    Optional<Pickle> scenario = feature.getPickles()
                            .stream().filter(t -> t.getLocation().getLine() == failure.getLineId())
                            .findFirst();

                    if (scenario.isPresent()) {
                        testId = testId + " - " + scenario.get().getName();
                    }
                }
                testFailures.append(testId).append("\n");
            });
            System.err.println(testFailures);
        }
    }
}
