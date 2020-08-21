package courgette.runtime.report.model;

import java.util.List;
import java.util.UUID;

public class Feature {
    private String courgetteFeatureId;
    private String name;
    private String uri;
    private List<Scenario> scenarios;

    public Feature(String name, String uri, List<Scenario> scenarios) {
        this.courgetteFeatureId = UUID.randomUUID().toString();
        this.name = name;
        this.uri = uri;
        this.scenarios = scenarios;
    }

    public String getCourgetteFeatureId() {
        return courgetteFeatureId;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public boolean passed() {
        return scenarios.stream().allMatch(Scenario::passed);
    }
}