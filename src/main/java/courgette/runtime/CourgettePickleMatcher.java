package courgette.runtime;

import cucumber.runtime.filter.Filters;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import gherkin.pickles.Compiler;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CourgettePickleMatcher {
    private final Compiler compiler;
    private final CucumberFeature cucumberFeature;
    private final Filters filters;

    public CourgettePickleMatcher(CucumberFeature cucumberFeature, Filters filters) {
        this.compiler = new Compiler();
        this.cucumberFeature = cucumberFeature;
        this.filters = filters;
    }

    public boolean matches() {
        AtomicBoolean matched = new AtomicBoolean();

        try {
            compiler.compile(cucumberFeature.getGherkinFeature()).forEach(pickle -> {
                matched.set(filters.matchesFilters(new PickleEvent(cucumberFeature.getUri().getSchemeSpecificPart(), pickle)));

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

        List<Pickle> pickles = compiler.compile(cucumberFeature.getGherkinFeature());

        try {
            pickles.forEach(pickle -> {
                PickleLocation pickleLocation = pickle.getLocations().stream().filter(l -> l.getLine() == pickleLocationLine).findFirst().orElse(null);

                if (pickleLocation != null) {
                    if (filters.matchesFilters(new PickleEvent(cucumberFeature.getUri().getSchemeSpecificPart(), pickle))) {
                        location[0] = pickleLocation;
                        throw new ConditionSatisfiedException();
                    }
                }
            });
        } catch (ConditionSatisfiedException ignored) {
        }

        return location[0];
    }

    private class ConditionSatisfiedException extends RuntimeException {
    }
}