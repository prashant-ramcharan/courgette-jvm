package courgette.runtime;

import courgette.runtime.utils.ReflectionUtils;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.model.CucumberFeature;

public class CourgetteFeature {
    private final FeatureRunner featureRunner;

    public CourgetteFeature(FeatureRunner featureRunner) {
        this.featureRunner = featureRunner;
    }

    public CucumberFeature getCucumberFeature() {
        final Object cucumberFeature = ReflectionUtils.classField.apply(featureRunner, "cucumberFeature");

        if (cucumberFeature != null) {
            return (CucumberFeature) cucumberFeature;
        }
        throw new CourgetteException("Unable to load cucumber feature.");
    }
}