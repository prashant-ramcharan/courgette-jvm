package courgette.runtime;

import cucumber.runner.EventBus;
import cucumber.runner.TimeService;
import cucumber.runner.TimeServiceEventBus;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.FeaturePathFeatureSupplier;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.filter.Filters;
import cucumber.runtime.filter.RerunFilters;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import gherkin.ast.Examples;
import gherkin.ast.ScenarioOutline;
import gherkin.pickles.PickleLocation;

import java.util.*;

public class CourgetteLoader {
    private final CourgetteProperties courgetteProperties;
    private final ResourceLoader resourceLoader;
    private final FeaturePathFeatureSupplier featureSupplier;
    private final RuntimeOptions runtimeOptions;
    private final EventBus eventBus;
    private final Filters filters;
    private final ClassFinder classFinder;

    private List<CucumberFeature> cucumberFeatures;

    public CourgetteLoader(CourgetteProperties courgetteProperties, ClassLoader classLoader) {
        this.courgetteProperties = courgetteProperties;
        this.resourceLoader = new MultiLoader(classLoader);
        final FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
        this.runtimeOptions = createRuntimeOptions();
        this.featureSupplier = new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);
        this.eventBus = new TimeServiceEventBus(TimeService.SYSTEM);
        final RerunFilters rerunFilters = new RerunFilters(runtimeOptions, featureLoader);
        this.filters = new Filters(runtimeOptions, rerunFilters);
        this.classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
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

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public ClassFinder getClassFinder() {
        return classFinder;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Filters getFilters() {
        return filters;
    }

    private RuntimeOptions createRuntimeOptions() {
        final CourgetteRuntimeOptions courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        final List<String> argv = Arrays.asList(courgetteRuntimeOptions.getRuntimeOptions());
        return new RuntimeOptions(argv);
    }

    private List<CucumberFeature> cucumberFeatures() {
        final List<CucumberFeature> loadedCucumberFeatures = featureSupplier.get();

        final List<CucumberFeature> matchedCucumberFeatures = new ArrayList<>();

        loadedCucumberFeatures.forEach(cucumberFeature -> {
            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, filters);

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
                            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, filters);

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