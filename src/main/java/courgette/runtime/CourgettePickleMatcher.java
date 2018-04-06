package courgette.runtime;

import cucumber.runtime.Runtime;
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
    private final Runtime runtime;

    public CourgettePickleMatcher(CucumberFeature cucumberFeature, Runtime runtime) {
        this.compiler = new Compiler();
        this.cucumberFeature = cucumberFeature;
        this.runtime = runtime;
    }

    public boolean matches() {
        AtomicBoolean matched = new AtomicBoolean();

        try {
            compiler.compile(cucumberFeature.getGherkinFeature()).forEach(pickle -> {
                matched.set(runtime.matchesFilters(new PickleEvent(cucumberFeature.getUri(), pickle)));

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
                    if (runtime.matchesFilters(new PickleEvent(cucumberFeature.getUri(), pickle))) {
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