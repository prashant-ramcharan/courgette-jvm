package courgette.runtime;

import gherkin.pickles.PickleLocation;
import io.cucumber.core.feature.CucumberFeature;
import io.cucumber.core.feature.FeatureLoader;
import io.cucumber.core.filter.Filters;
import io.cucumber.core.io.MultiLoader;
import io.cucumber.core.io.ResourceLoader;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourgetteLoader {
    private final CourgetteProperties courgetteProperties;
    private final List<CucumberFeature> cucumberFeatures;
    private final Filters filters;

    public CourgetteLoader(CourgetteProperties courgetteProperties, ClassLoader classLoader) {
        this.courgetteProperties = courgetteProperties;

        RuntimeOptions runtimeOptions = createRuntimeOptions();
        this.filters = new Filters(runtimeOptions);

        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
        FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);
        this.cucumberFeatures = featureSupplier.get();
    }

    public List<CucumberFeature> getCucumberFeatures() {
        return filterCucumberFeatures();
    }

    public Map<PickleLocation, CucumberFeature> getCucumberScenarios() {
        return filterCucumberScenarios(cucumberFeatures);
    }

    private RuntimeOptions createRuntimeOptions() {
        return new CourgetteRuntimeOptions(courgetteProperties).getRuntimeOptions();
    }

    private List<CucumberFeature> filterCucumberFeatures() {
        final List<CucumberFeature> matchedCucumberFeatures = new ArrayList<>();

        cucumberFeatures.forEach(cucumberFeature -> {
            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, filters);

            if (pickleMatcher.matches()) {
                matchedCucumberFeatures.add(cucumberFeature);
            }
        });
        return matchedCucumberFeatures;
    }

    private Map<PickleLocation, CucumberFeature> filterCucumberScenarios(List<CucumberFeature> cucumberFeatures) {
        final Map<PickleLocation, CucumberFeature> scenarios = new HashMap<>();

        if (cucumberFeatures != null) {
            cucumberFeatures.forEach(cucumberFeature ->
                    cucumberFeature.getPickles().forEach(pickle -> {
                        CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(cucumberFeature, filters);

                        PickleLocation pickleLocation = pickleMatcher.matchLocation(pickle.getLine());
                        if (pickleLocation != null) {
                            scenarios.put(pickleLocation, cucumberFeature);
                        }
                    }));
        }
        return scenarios;
    }
}