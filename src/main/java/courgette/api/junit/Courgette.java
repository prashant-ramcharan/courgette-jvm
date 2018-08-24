package courgette.api.junit;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.*;
import cucumber.runner.EventBus;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import gherkin.pickles.PickleLocation;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Courgette extends ParentRunner<FeatureRunner> {
    private final CourgetteFeatureLoader courgetteFeatureLoader;
    private final CourgetteProperties courgetteProperties;
    private final List<CucumberFeature> cucumberFeatures;
    private final List<CourgetteRunnerInfo> runnerInfoList;

    public Courgette(Class clazz) throws InitializationError {
        super(clazz);

        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(clazz);
        courgetteProperties = new CourgetteProperties(courgetteOptions, createSessionId(), courgetteOptions.threads());

        courgetteFeatureLoader = new CourgetteFeatureLoader(courgetteProperties, clazz.getClassLoader());
        cucumberFeatures = courgetteFeatureLoader.getCucumberFeatures();

        runnerInfoList = new ArrayList<>();

        if (courgetteOptions.runLevel().equals(CourgetteRunLevel.FEATURE)) {
            cucumberFeatures.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<PickleLocation, CucumberFeature> scenarios = courgetteFeatureLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(location -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenarios.get(location), location.getLine())));
        }
    }

    @Override
    public List<FeatureRunner> getChildren() {
        final Runtime runtime = courgetteFeatureLoader.getRuntime();
        final RuntimeOptions runtimeOptions = courgetteFeatureLoader.getRuntimeOptions();
        final EventBus eventBus = runtime.getEventBus();

        final JUnitReporter jUnitReporter = new JUnitReporter(eventBus, runtimeOptions.isStrict(), new JUnitOptions(runtimeOptions.getJunitOptions()));

        final List<FeatureRunner> children = new ArrayList<>();
        this.cucumberFeatures.forEach(cucumberFeature -> {
            try {
                FeatureRunner runner = new FeatureRunner(cucumberFeature, runtime, jUnitReporter);
                runner.getDescription();
                children.add(runner);
            } catch (InitializationError error) {
                error.printStackTrace();
            }
        });
        return children;
    }

    @Override
    protected Description describeChild(FeatureRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(FeatureRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    public void run(RunNotifier notifier) {
        final CourgetteRunner courgetteRunner = new CourgetteRunner(runnerInfoList, courgetteProperties);

        if (courgetteRunner.canRunFeatures()) {
            courgetteRunner.run();
            courgetteRunner.createReport();
            courgetteRunner.createCourgetteReport();
        }

        if (courgetteRunner.hasFailures()) {
            courgetteRunner.createRerunFile();
            throw new CourgetteTestFailureException("There were failing tests. Refer to the Courgette html report for more details.");
        }
    }

    private String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}