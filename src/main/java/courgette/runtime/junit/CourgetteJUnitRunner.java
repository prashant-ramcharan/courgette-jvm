package courgette.runtime.junit;

import courgette.api.CourgetteRunLevel;
import courgette.runtime.*;
import io.cucumber.core.feature.CucumberFeature;
import io.cucumber.core.feature.CucumberPickle;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class CourgetteJUnitRunner extends ParentRunner<CourgetteRunnerInfo> {
    protected CourgetteProperties courgetteProperties;
    protected List<CucumberFeature> cucumberFeatures;
    protected List<CourgetteRunnerInfo> runnerInfoList;
    protected CourgetteCallbacks callbacks;
    protected Description description;

    private List<TestDescription> testDescriptions = new ArrayList<>();

    protected CourgetteJUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected List<CourgetteRunnerInfo> getChildren() {
        return runnerInfoList;
    }

    @Override
    protected Description describeChild(CourgetteRunnerInfo child) {
        return description;
    }

    @Override
    public Description getDescription() {
        createDescription();
        return description;
    }

    @Override
    protected void runChild(CourgetteRunnerInfo child, RunNotifier notifier) {
    }

    protected void notifyTestStarted(RunNotifier notifier) {
        description.getChildren().forEach(t -> {
            notifier.fireTestStarted(t);
            t.getChildren().forEach(notifier::fireTestStarted);
        });
    }

    protected List<TestDescription> notifyTestFailure(RunNotifier notifier, List<CourgetteRunResult> runResults) {
        List<TestDescription> failedTestDescriptions = new ArrayList<>();

        runResults.stream().filter(r -> CourgetteRunResult.Status.FAILED.equals(r.getStatus()))
                .collect(Collectors.toList())
                .forEach(r ->
                {
                    CucumberFeature cucumberFeature = r.getCucumberFeature();

                    if (isFeatureRunLevel()) {
                        TestDescription testDescription = testDescriptions.stream().filter(t -> t.getCucumberFeature().equals(cucumberFeature)).findFirst().get();

                        notifier.fireTestFailure(new Failure(testDescription.getDescription(), new CourgetteTestFailureException("Please refer to Courgette / Cucumber report for more info.")));
                        failedTestDescriptions.add(testDescription);
                    } else {
                        TestDescription testDescription = testDescriptions.stream().filter(t -> t.getCucumberFeature().equals(cucumberFeature)).findFirst().get();
                        Description pickleDescription = testDescription.getDescription().getChildren().stream().filter(c -> c.getDisplayName().contains("(line " + r.getLineId() + ")")).findFirst().get();

                        notifier.fireTestFailure(new Failure(pickleDescription, new CourgetteTestFailureException("Please refer to Courgette / Cucumber report for more info.")));
                        failedTestDescriptions.add(testDescription);

                    }
                });

        return failedTestDescriptions;
    }

    protected void notifyTestSuccess(RunNotifier notifier, List<TestDescription> failures) {
        testDescriptions.removeAll(failures);

        if (isFeatureRunLevel()) {
            testDescriptions.forEach(testDescription -> notifier.fireTestFinished(testDescription.getDescription()));
        } else {
            testDescriptions.forEach(testDescription -> testDescription.getDescription().getChildren().forEach(notifier::fireTestFinished));
        }
    }

    protected String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private void createDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName());
        }

        if (testDescriptions.isEmpty()) {
            if (CourgetteRunLevel.FEATURE == courgetteProperties.getCourgetteOptions().runLevel()) {
                getChildren().forEach(runnerInfo -> {
                    CucumberFeature cucumberFeature = runnerInfo.getCucumberFeature();

                    Description featureDescription = Description.createTestDescription("", cucumberFeature.getName());
                    description.addChild(featureDescription);
                    testDescriptions.add(new TestDescription(featureDescription, cucumberFeature));
                });
            } else {
                AtomicReference<CucumberFeature> cucumberFeature = null;

                getChildren().forEach(runnerInfo -> {
                    Description featureDescription = null;

                    if (cucumberFeature == null || cucumberFeature.get() != runnerInfo.getCucumberFeature()) {
                        featureDescription = Description.createSuiteDescription(cucumberFeature.get().getName());
                        cucumberFeature.set(runnerInfo.getCucumberFeature());
                    }

                    for (CucumberPickle cucumberPickle : cucumberFeature.get().getPickles()) {
                        if (cucumberPickle.getLine() == runnerInfo.getLineId()) {
                            Description scenario = Description.createTestDescription("", cucumberPickle.getName() + " (line " + cucumberPickle.getLine() + ")");
                            featureDescription.addChild(scenario);
                            testDescriptions.add(new TestDescription(featureDescription, cucumberFeature.get()));
                        }
                    }
                    description.addChild(featureDescription);
                });
            }
        }
    }

    private boolean isFeatureRunLevel() {
        return CourgetteRunLevel.FEATURE == courgetteProperties.getCourgetteOptions().runLevel();
    }
}
