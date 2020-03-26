package courgette.runtime.junit;

import courgette.runtime.CourgetteCallbacks;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteTestFailureException;
import io.cucumber.core.gherkin.Feature;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class CourgetteJUnitRunner extends ParentRunner<Feature> {
    protected CourgetteProperties courgetteProperties;
    protected List<Feature> features;
    protected List<CourgetteRunnerInfo> runnerInfoList;
    protected CourgetteCallbacks callbacks;
    protected Description description;

    private Map<Feature, Description> featureDescriptions = new HashMap<>();
    private List<String> featureDescriptionNames = new ArrayList<>();

    protected CourgetteJUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected List<Feature> getChildren() {
        return features;
    }

    @Override
    protected Description describeChild(Feature child) {
        return description;
    }

    @Override
    public Description getDescription() {
        createDescription();
        return description;
    }

    @Override
    protected void runChild(Feature child, RunNotifier notifier) {
    }

    protected String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private void createDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName());
        }

        getChildren()
                .stream()
                .distinct()
                .forEach(feature -> {
                    if (!featureDescriptionNames.contains(feature.getName())) {
                        Description featureDescription = Description.createTestDescription("", feature.getName());
                        description.addChild(featureDescription);
                        featureDescriptions.put(feature, featureDescription);
                        featureDescriptionNames.add(feature.getName());
                    }
                });
    }

    protected void notifyTestStarted(RunNotifier notifier) {
        featureDescriptions.values().forEach(notifier::fireTestStarted);
    }

    protected void notifyTestSuccess(RunNotifier notifier) {
        featureDescriptions.values().forEach(notifier::fireTestFinished);
    }

    protected void notifyTestFailure(RunNotifier notifier, List<CourgetteRunResult> failures) {
        failures.forEach(failure -> {
            Feature feature = failure.getFeature();
            Description description = featureDescriptions.get(feature);
            notifier.fireTestFailure(new Failure(description, new CourgetteTestFailureException("Please refer to Courgette / Cucumber report for more info.")));
        });
        featureDescriptions.keySet().removeAll(failures.stream().map(CourgetteRunResult::getFeature).collect(Collectors.toList()));
    }
}
