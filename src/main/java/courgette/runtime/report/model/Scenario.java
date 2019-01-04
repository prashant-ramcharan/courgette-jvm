package courgette.runtime.report.model;

import java.util.List;
import java.util.UUID;

public class Scenario {
    private String courgetteScenarioId;
    private String featureUri;
    private String name;
    private int line;
    private String keyword;
    private List<Hook> before;
    private List<Hook> after;
    private List<Step> steps;

    public Scenario(String featureUri, String name, String keyword, int line, List<Hook> before, List<Hook> after, List<Step> steps) {
        this.courgetteScenarioId = UUID.randomUUID().toString();
        this.featureUri = featureUri;
        this.name = name;
        this.keyword = keyword;
        this.line = line;
        this.before = before;
        this.after = after;
        this.steps = steps;
    }

    public String getCourgetteScenarioId() {
        return courgetteScenarioId;
    }

    public String getFeatureUri() {
        return featureUri;
    }

    public String getName() {
        return name;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getLine() {
        return line;
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

    public boolean passed(boolean isStrict) {
        return before.stream().allMatch(before -> before.passed(isStrict))
                && after.stream().allMatch(after -> after.passed(isStrict))
                && steps.stream().allMatch(step -> step.passed(isStrict));
    }
}
