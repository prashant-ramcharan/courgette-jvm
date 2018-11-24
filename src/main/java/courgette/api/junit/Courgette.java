package courgette.api.junit;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.*;
import cucumber.runner.EventBus;
import cucumber.runner.ThreadLocalRunnerSupplier;
import cucumber.runtime.*;
import cucumber.runtime.filter.Filters;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
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
    private final CourgetteLoader courgetteLoader;
    private final CourgetteProperties courgetteProperties;
    private final List<CucumberFeature> cucumberFeatures;
    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final CourgetteCallbacks callbacks;

    public Courgette(Class clazz) throws InitializationError {
        super(clazz);

        final CourgetteOptions courgetteOptions = new CourgetteRunOptions(clazz);
        courgetteProperties = new CourgetteProperties(courgetteOptions, createSessionId(), courgetteOptions.threads());

        callbacks = new CourgetteCallbacks(clazz);

        courgetteLoader = new CourgetteLoader(courgetteProperties, clazz.getClassLoader());
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
    public List<FeatureRunner> getChildren() {
        final RuntimeOptions runtimeOptions = courgetteLoader.getRuntimeOptions();
        final EventBus eventBus = courgetteLoader.getEventBus();
        final ResourceLoader resourceLoader = courgetteLoader.getResourceLoader();
        final ClassFinder classFinder = courgetteLoader.getClassFinder();
        final Filters filters = courgetteLoader.getFilters();

        final JUnitOptions jUnitOptions = new JUnitOptions(runtimeOptions.isStrict(), runtimeOptions.getJunitOptions());
        final BackendSupplier backendSupplier = new BackendModuleBackendSupplier(resourceLoader, classFinder, runtimeOptions);
        final ThreadLocalRunnerSupplier runnerSupplier = new ThreadLocalRunnerSupplier(runtimeOptions, eventBus, backendSupplier);

        final List<FeatureRunner> children = new ArrayList<>();
        this.cucumberFeatures.forEach(cucumberFeature -> {
            try {
                FeatureRunner runner = new FeatureRunner(cucumberFeature, filters, runnerSupplier, jUnitOptions);
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

        try {
            callbacks.beforeAll();

            if (courgetteRunner.canRunFeatures()) {
                courgetteRunner.run();
                courgetteRunner.createReport();
                courgetteRunner.createCourgetteReport();
            }

            if (courgetteRunner.hasFailures()) {
                courgetteRunner.createRerunFile();
                throw new CourgetteTestFailureException("There were failing tests. Refer to the Courgette html report for more details.");
            }
        } finally {
            callbacks.afterAll();
        }
    }

    private String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}