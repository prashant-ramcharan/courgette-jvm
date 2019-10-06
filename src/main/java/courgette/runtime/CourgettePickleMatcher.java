package courgette.runtime;

import gherkin.pickles.PickleLocation;
import io.cucumber.core.feature.CucumberFeature;
import io.cucumber.core.feature.CucumberPickle;
import io.cucumber.core.filter.Filters;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CourgettePickleMatcher {
    private final CucumberFeature cucumberFeature;
    private final Filters filters;

    public CourgettePickleMatcher(CucumberFeature cucumberFeature, Filters filters) {
        this.cucumberFeature = cucumberFeature;
        this.filters = filters;
    }

    public boolean matches() {
        AtomicBoolean matched = new AtomicBoolean();

        try {
            cucumberFeature.getPickles().forEach(pickle -> {
                matched.set(filters.test(pickle));
                if (matched.get()) {
                    throw new ConditionSatisfiedException();
                }
            });
        } catch (ConditionSatisfiedException ignored) {
        }
        return matched.get();
    }

    public PickleLocation matchLocation(int pickleLocationLine) {
        final PickleLocation[] location = {null};

        List<CucumberPickle> pickles = cucumberFeature.getPickles();

        try {
            pickles.stream().filter(p -> p.getLine() == pickleLocationLine)
                    .findFirst()
                    .ifPresent(pickleEvent -> {
                        if (filters.test(pickleEvent)) {
                            location[0] = new PickleLocation(pickleEvent.getLine(), pickleEvent.getScenarioLine());
                            throw new ConditionSatisfiedException();
                        }
                    });
        } catch (ConditionSatisfiedException ignored) {
        }
        return location[0];
    }

    private class ConditionSatisfiedException extends RuntimeException {
    }
}