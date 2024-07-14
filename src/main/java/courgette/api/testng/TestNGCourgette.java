package courgette.api.testng;

import courgette.api.CourgetteOptions;
import courgette.runtime.CourgetteLoader;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunOptions;
import courgette.runtime.CourgetteRunner;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteSession;
import courgette.runtime.CourgetteTestErrorException;
import courgette.runtime.CourgetteTestFailureException;
import courgette.runtime.CucumberPickleLocation;
import io.cucumber.core.gherkin.Feature;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TestNGCourgette {
    private CourgetteProperties courgetteProperties;
    private List<CourgetteRunnerInfo> runnerInfoList;

    @BeforeClass(alwaysRun = true)
    public void initialize() {
        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(this.getClass());
        courgetteProperties = new CourgetteProperties(courgetteOptions, CourgetteSession.current().sessionId(), courgetteOptions.threads());

        CourgetteLoader courgetteFeatureLoader = new CourgetteLoader(courgetteProperties);
        List<Feature> features = courgetteFeatureLoader.getFeatures();

        runnerInfoList = new ArrayList<>();

        if (courgetteProperties.isFeatureRunLevel()) {
            features.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<CucumberPickleLocation, Feature> scenarios = courgetteFeatureLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(location -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenarios.get(location), location.getLine())));
        }
    }

    @Test
    public void parallelRun() {
        final CourgetteRunner courgetteRunner = new CourgetteRunner(runnerInfoList, courgetteProperties);

        try {
            if (courgetteRunner.canRunFeatures()) {
                switch (courgetteRunner.run()) {
                    case OK:
                        courgetteRunner.createCucumberReport();
                        courgetteRunner.createCourgetteReport();
                        courgetteRunner.createErrorReport();
                        courgetteRunner.createCourgettePluginReports();
                        courgetteRunner.createCourgetteRunLogFile();
                        break;
                    case ERROR:
                        CourgetteTestErrorException.throwTestErrorException();
                }

                if (!courgetteRunner.getFailures().isEmpty()) {
                    courgetteRunner.createRerunFile();
                    throw new CourgetteTestFailureException("There were failing tests.");
                }
            }
        } finally {
            courgetteRunner.printCourgetteTestStatistics();
            courgetteRunner.printCourgetteTestFailures();
        }
    }
}