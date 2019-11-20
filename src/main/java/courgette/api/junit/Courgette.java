package courgette.api.junit;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.*;
import courgette.runtime.junit.CourgetteJUnitRunner;
import gherkin.pickles.PickleLocation;
import io.cucumber.core.feature.CucumberFeature;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.Map;

public class Courgette extends CourgetteJUnitRunner {

    public Courgette(Class clazz) throws InitializationError {
        super(clazz);

        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(clazz);
        courgetteProperties = new CourgetteProperties(courgetteOptions, createSessionId(), courgetteOptions.threads());

        callbacks = new CourgetteCallbacks(clazz);

        final CourgetteLoader courgetteLoader = new CourgetteLoader(courgetteProperties, clazz.getClassLoader());
        cucumberFeatures = courgetteLoader.getCucumberFeatures();

        runnerInfoList = new ArrayList<>();

        if (courgetteOptions.runLevel().equals(CourgetteRunLevel.FEATURE)) {
            cucumberFeatures.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<PickleLocation, CucumberFeature> scenarios = courgetteLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(location -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenarios.get(location), location.getLine())));
        }
    }

    @Override
    public void run(RunNotifier notifier) {
        final CourgetteRunner courgetteRunner = new CourgetteRunner(runnerInfoList, courgetteProperties);

        try {
            callbacks.beforeAll();

            if (courgetteRunner.canRunFeatures()) {
                courgetteRunner.run();
                courgetteRunner.createReport();
                courgetteRunner.createCourgetteReport();

                if (courgetteProperties.isReportPortalPluginEnabled()) {
                    courgetteRunner.publishReportToReportPortal();
                }
            }

            if (courgetteRunner.hasFailures()) {
                courgetteRunner.createRerunFile();
            }
        } finally {
            callbacks.afterAll();

            notifyTestStarted(notifier);
            notifyTestFailure(notifier, courgetteRunner.getFailures());
            notifyTestSuccess(notifier);
        }
    }
}