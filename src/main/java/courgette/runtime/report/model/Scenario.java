package courgette.runtime.report.model;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Scenario {
    private String courgetteScenarioId;
    private String name;
    private List<Hook> before;
    private List<Hook> after;
    private List<Step> steps;

    public Scenario(String name, List<Hook> before, List<Hook> after, List<Step> steps) {
        this.courgetteScenarioId = UUID.randomUUID().toString();
        this.name = name;
        this.before = before;
        this.after = after;
        this.steps = steps;
    }

    public String getCourgetteScenarioId() {
        return courgetteScenarioId;
    }

    public String getName() {
        return name;
    }

    public List<Hook> getBefore() {
        return before;
    }

    public List<Hook> getAfter() {
        return after;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public boolean passed() {
        final AtomicInteger stepsPassed = new AtomicInteger(0);

        steps.forEach(step -> {
            if (stepsPassed.get() > -1 && stepsPassed.get() < steps.size()) {

                if (step.failed()) {
                    stepsPassed.set(-1);
                }

                if (step.isAmbiguous()) {
                    stepsPassed.set(steps.size());
                } else {
                    stepsPassed.incrementAndGet();
                }
            }
        });

        return before.stream().allMatch(Hook::passed) && after.stream().allMatch(Hook::passed) && (stepsPassed.get() == steps.size());
    }
}
