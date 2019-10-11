package courgette.runtime.junit;

import courgette.runtime.*;
import io.cucumber.core.feature.CucumberFeature;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class CourgetteJUnitRunner extends ParentRunner<CucumberFeature> {
    protected CourgetteProperties courgetteProperties;
    protected List<CucumberFeature> cucumberFeatures;
    protected List<CourgetteRunnerInfo> runnerInfoList;
    protected CourgetteCallbacks callbacks;
    protected Description description;

    private Map<CucumberFeature, Description> cucumberFeatureDescriptions = new HashMap<>();

    protected CourgetteJUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected List<CucumberFeature> getChildren() {
        return cucumberFeatures;
    }

    @Override
    protected Description describeChild(CucumberFeature child) {
        return description;
    }

    @Override
    public Description getDescription() {
        createDescription();
        return description;
    }

    @Override
    protected void runChild(CucumberFeature child, RunNotifier notifier) {
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
                .forEach(cucumberFeature -> {
                    Description featureDescription = Description.createTestDescription("", cucumberFeature.getName());
                    description.addChild(featureDescription);
                    cucumberFeatureDescriptions.put(cucumberFeature, featureDescription);
                });
    }

    protected void notifyTestStarted(RunNotifier notifier) {
        cucumberFeatureDescriptions.values().forEach(notifier::fireTestStarted);
    }

    protected void notifyTestSuccess(RunNotifier notifier) {
        cucumberFeatureDescriptions.values().forEach(notifier::fireTestFinished);
    }

    protected void notifyTestFailure(RunNotifier notifier, List<CourgetteRunResult> failures) {
        failures.forEach(failure -> {
            CucumberFeature cucumberFeature = failure.getCucumberFeature();
            Description description = cucumberFeatureDescriptions.get(cucumberFeature);
            notifier.fireTestFailure(new Failure(description, new CourgetteTestFailureException("Please refer to Courgette / Cucumber report for more info.")));
        });
        cucumberFeatureDescriptions.keySet().removeAll(failures.stream().map(CourgetteRunResult::getCucumberFeature).collect(Collectors.toList()));
    }
}
