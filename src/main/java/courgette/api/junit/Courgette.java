package courgette.api.junit;

import courgette.api.CourgetteOptions;
import courgette.runtime.CourgetteCallbacks;
import courgette.runtime.CourgetteLoader;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunOptions;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunner;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteSession;
import courgette.runtime.CourgetteTestErrorException;
import courgette.runtime.CucumberPickleLocation;
import courgette.runtime.RunStatus;
import courgette.runtime.junit.CourgetteJUnitRunner;
import io.cucumber.core.gherkin.Feature;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Courgette extends CourgetteJUnitRunner {

    public Courgette(Class clazz) throws InitializationError {
        super(clazz);

        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(clazz);
        courgetteProperties = new CourgetteProperties(courgetteOptions, CourgetteSession.current().sessionId(), courgetteOptions.threads());

        callbacks = new CourgetteCallbacks(clazz);

        final CourgetteLoader courgetteLoader = new CourgetteLoader(courgetteProperties);
        features = courgetteLoader.getFeatures();

        runnerInfoList = new ArrayList<>();

        if (courgetteProperties.isFeatureRunLevel()) {
            features.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<CucumberPickleLocation, Feature> scenarios = courgetteLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(location -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenarios.get(location), location.getLine())));
        }
    }

    @Override
    public void run(RunNotifier notifier) {
        final CourgetteRunner courgetteRunner = new CourgetteRunner(runnerInfoList, courgetteProperties);

        RunStatus runStatus = RunStatus.ERROR;

        List<CourgetteRunResult> failures = new ArrayList<>();

        try {
            callbacks.beforeAll();

            if (courgetteRunner.canRunFeatures()) {

                runStatus = courgetteRunner.run();

                if (RunStatus.OK.equals(runStatus)) {
                    courgetteRunner.createCucumberReport();
                    courgetteRunner.createCourgetteReport();

                    if (courgetteProperties.isExtentReportsPluginEnabled()) {
                        courgetteRunner.createCourgetteExtentReports();
                    }

                    failures = courgetteRunner.getFailures();

                    if (!failures.isEmpty()) {
                        courgetteRunner.createRerunFile();
                    }
                } else {
                    CourgetteTestErrorException.throwTestErrorException();
                }
            }
        } finally {
            courgetteRunner.printCourgetteTestStatistics();
            courgetteRunner.cleanupCourgetteHtmlReportFiles();
            callbacks.afterAll();
            notifyTestStarted(notifier);
            notifyTestFailure(notifier, failures, runStatus);
            notifyTestSuccess(notifier);
        }
    }
}