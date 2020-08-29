package courgette.api.testng;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.CourgetteLoader;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunOptions;
import courgette.runtime.CourgetteRunner;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteTestFailureException;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.gherkin.Location;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class TestNGCourgette {
    private CourgetteProperties courgetteProperties;
    private List<CourgetteRunnerInfo> runnerInfoList;

    @BeforeClass(alwaysRun = true)
    public void initialize() {
        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(this.getClass());
        courgetteProperties = new CourgetteProperties(courgetteOptions, createSessionId(), courgetteOptions.threads());

        CourgetteLoader courgetteFeatureLoader = new CourgetteLoader(courgetteProperties);
        List<Feature> features = courgetteFeatureLoader.getFeatures();

        runnerInfoList = new ArrayList<>();

        if (courgetteOptions.runLevel().equals(CourgetteRunLevel.FEATURE)) {
            features.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<Location, Feature> scenarios = courgetteFeatureLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(location -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenarios.get(location), location.getLine())));
        }
    }

    @Test
    public void parallelRun() {
        final CourgetteRunner courgetteRunner = new CourgetteRunner(runnerInfoList, courgetteProperties);

        if (courgetteRunner.canRunFeatures()) {
            courgetteRunner.run();
            courgetteRunner.createReport();
            courgetteRunner.createCourgetteReport();

            if (courgetteProperties.isCucumberReportPublisherEnabled()) {
                courgetteRunner.publishCucumberReport();
            }

            if (courgetteProperties.isExtentReportsPluginEnabled()) {
                courgetteRunner.createCourgetteExtentReports();
            }

            if (courgetteProperties.isReportPortalPluginEnabled()) {
                courgetteRunner.publishReportToReportPortal();
            }
        }

        if (courgetteRunner.hasFailures()) {
            courgetteRunner.createRerunFile();
            courgetteRunner.cleanupCourgetteHtmlReportFiles();
            throw new CourgetteTestFailureException("There were failing tests. Refer to the Courgette html report for more details.");
        }
    }

    private String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}