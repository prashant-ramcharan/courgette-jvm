package courgette.runtime.report.model;

import java.util.List;
import java.util.UUID;

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
}
