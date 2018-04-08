package courgette.runtime;

import cucumber.runner.EventBus;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.ast.Examples;
import gherkin.ast.ScenarioOutline;
import gherkin.pickles.PickleLocation;

import java.util.*;

public class CourgetteFeatureLoader {
    private final CourgetteProperties courgetteProperties;
    private final ResourceLoader resourceLoader;
    private final RuntimeOptions runtimeOptions;
    private final Runtime runtime;
    private final EventBus eventBus;

    private List<CucumberFeature> cucumberFeatures;

    public CourgetteFeatureLoader(CourgetteProperties courgetteProperties, ClassLoader classLoader) {
        this.courgetteProperties = courgetteProperties;
        this.resourceLoader = new MultiLoader(classLoader);
        this.runtimeOptions = createRuntimeOptions();
        this.runtime = createRuntime(runtimeOptions, classLoader);
        this.eventBus = runtime.getEventBus();
    }

    public List<CucumberFeature> getCucumberFeatures() {
        cucumberFeatures = cucumberFeatures();
        return cucumberFeatures;
    }

    public Map<PickleLocation, CucumberFeature> getCucumberScenarios() {
        return cucumberScenarios(cucumberFeatures);
    }

    public RuntimeOptions getRuntimeOptions() {
        return runtimeOptions;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    private RuntimeOptions createRuntimeOptions() {
        final CourgetteRuntimeOptions courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        final List<String> argv = Arrays.asList(courgetteRuntimeOptions.getRuntimeOptions());
        return new RuntimeOptions(argv);
    }

    private Runtime createRuntime(RuntimeOptions runtimeOptions, ClassLoader classLoader) {
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    private List<CucumberFeature> cucumberFeatures() {
        final List<CucumberFeature> loadedCucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader, eventBus);

        final List<CucumberFeature> matchedCucumberFeatures = new ArrayList<>();

        loadedCucumberFeatures.forEach(cucumberFeature -> {
            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, runtime);

            if (pickleMatcher.matches()) {
                matchedCucumberFeatures.add(cucumberFeature);
            }
        });
        return matchedCucumberFeatures;
    }

    private Map<PickleLocation, CucumberFeature> cucumberScenarios(List<CucumberFeature> cucumberFeatures) {
        final Map<PickleLocation, CucumberFeature> scenarios = new HashMap<>();

        if (cucumberFeatures != null) {
            cucumberFeatures.forEach(cucumberFeature ->
                    cucumberFeature.getGherkinFeature().getFeature().getChildren().forEach(scenario -> {

                        final List<Integer> lines = new ArrayList<>();

                        if (scenario instanceof ScenarioOutline) {
                            List<Examples> examples = ((ScenarioOutline) scenario).getExamples();

                            examples.forEach(example ->
                                    example.getTableBody().forEach(
                                            tr -> lines.add(tr.getLocation().getLine())
                                    ));
                        } else {
                            lines.add(scenario.getLocation().getLine());
                        }

                        lines.forEach(line -> {
                            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, runtime);

                            PickleLocation pickleLocation = pickleMatcher.matchLocation(line);

                            if (pickleLocation != null) {
                                scenarios.put(pickleLocation, cucumberFeature);
                            }
                        });
                    }));
        }
        return scenarios;
    }
}