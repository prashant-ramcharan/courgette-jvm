package courgette.runtime.junit;

import io.cucumber.core.feature.CucumberFeature;
import org.junit.runner.Description;

public class TestDescription {
    private Description description;
    private CucumberFeature cucumberFeature;

    public TestDescription(Description description, CucumberFeature cucumberFeature) {
        this.description = description;
        this.cucumberFeature = cucumberFeature;
    }

    public Description getDescription() {
        return description;
    }

    public CucumberFeature getCucumberFeature() {
        return cucumberFeature;
    }
}
