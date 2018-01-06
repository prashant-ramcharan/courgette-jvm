package courgette.api.junit;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.*;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.*;

public class Courgette extends ParentRunner<FeatureRunner> {
    private final CourgetteProperties courgetteProperties;
    private final List<CucumberFeature> cucumberFeatures;
    private final List<CourgetteRunnerInfo> runnerInfoList;
    private final ClassLoader classLoader;
    private final RuntimeOptions runtimeOptions;

    public Courgette(Class clazz) throws IOException, InitializationError {
        super(clazz);
        classLoader = clazz.getClassLoader();

        final CourgetteOptions courgetteOptions = getCourgetteOptions(clazz);
        courgetteProperties = new CourgetteProperties(courgetteOptions, createSessionId(), courgetteOptions.threads());

        final CourgetteFeatureLoader courgetteFeatureLoader = new CourgetteFeatureLoader(courgetteProperties);
        cucumberFeatures = courgetteFeatureLoader.getCucumberFeatures();

        runtimeOptions = courgetteFeatureLoader.getCucumberRuntimeOptions();

        runnerInfoList = new ArrayList<>();

        if (courgetteOptions.runLevel().equals(CourgetteRunLevel.FEATURE)) {
            cucumberFeatures.forEach(feature -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, feature, null)));
        } else {
            final Map<CucumberFeature, Integer> scenarios = courgetteFeatureLoader.getCucumberScenarios();
            scenarios
                    .keySet()
                    .forEach(scenario -> runnerInfoList.add(new CourgetteRunnerInfo(courgetteProperties, scenario, scenarios.get(scenario))));
        }
    }

    @Override
    public List<FeatureRunner> getChildren() {
        final Runtime runtime = createRuntime(runtimeOptions);
        final JUnitReporter jUnitReporter = new JUnitReporter(runtimeOptions.reporter(classLoader), runtimeOptions.formatter(classLoader), runtimeOptions.isStrict(), new JUnitOptions(runtimeOptions.getJunitOptions()));

        final List<FeatureRunner> children = new ArrayList<>();
        this.cucumberFeatures.forEach(cucumberFeature -> {
            try {
                children.add(new FeatureRunner(cucumberFeature, runtime, jUnitReporter));
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

        if (courgetteRunner.allFeaturesPassed()) {
            System.exit(0x0);
        } else {
            courgetteRunner.createRerunFile();
            System.exit(0x1);
        }
    }

    private String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private CourgetteOptions getCourgetteOptions(Class clazz) {
        return (CourgetteOptions)
                Arrays.stream(clazz.getDeclaredAnnotations())
                        .filter(annotation -> annotation.annotationType().equals(CourgetteOptions.class))
                        .findFirst()
                        .orElseThrow(() -> new CourgetteException("Class is not annotated with @CourgetteOptions"));
    }

    private Runtime createRuntime(RuntimeOptions runtimeOptions) {
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }
}