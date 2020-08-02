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
    private List<Tag> tags;
    private String startTimestamp;

    public Scenario(String featureUri,
                    String startTimestamp,
                    String name,
                    String keyword,
                    int line,
                    List<Hook> before,
                    List<Hook> after,
                    List<Step> steps,
                    List<Tag> tags) {
        this.courgetteScenarioId = UUID.randomUUID().toString();
        this.featureUri = featureUri;
        this.startTimestamp = startTimestamp;
        this.name = name;
        this.keyword = keyword;
        this.line = line;
        this.before = before;
        this.after = after;
        this.steps = steps;
        this.tags = tags;
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

    public List<Tag> getTags() {
        return tags;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public boolean passed() {
        return before.stream().allMatch(Hook::passed)
                && after.stream().allMatch(Hook::passed)
                && steps.stream().allMatch(Step::passed);
    }
}
