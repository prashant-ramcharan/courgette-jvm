package courgette.runtime;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.filter.Filters;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.resource.ClassLoaders;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;
import io.cucumber.core.runtime.TimeServiceEventBus;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CourgetteLoader {
    private final CourgetteProperties courgetteProperties;
    private final List<Feature> features;
    private final Filters filters;

    public CourgetteLoader(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;

        RuntimeOptions runtimeOptions = createRuntimeOptions();
        this.filters = new Filters(runtimeOptions);

        EventBus eventBus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
        FeatureParser parser = new FeatureParser(eventBus::generateId);
        Supplier<ClassLoader> classLoader = ClassLoaders::getDefaultClassLoader;
        FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(classLoader, runtimeOptions, parser);

        this.features = featureSupplier.get();
    }

    public List<Feature> getFeatures() {
        return filterFeatures();
    }

    public Map<CucumberPickleLocation, Feature> getCucumberScenarios() {
        return filterCucumberScenarios(features);
    }

    private RuntimeOptions createRuntimeOptions() {
        return new CourgetteRuntimeOptions(courgetteProperties).getRuntimeOptions();
    }

    private List<Feature> filterFeatures() {
        final List<Feature> matchedFeatures = new ArrayList<>();

        features.forEach(feature -> {
            CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(feature, filters);

            if (pickleMatcher.matches()) {
                matchedFeatures.add(feature);
            }
        });
        return matchedFeatures;
    }

    private Map<CucumberPickleLocation, Feature> filterCucumberScenarios(List<Feature> features) {
        final Map<CucumberPickleLocation, Feature> scenarios = new HashMap<>();

        if (features != null) {
            features.forEach(feature ->
                    feature.getPickles().forEach(pickle -> {
                        CourgettePickleMatcher pickleMatcher = new CourgettePickleMatcher(feature, filters);

                        CucumberPickleLocation pickleLocation = pickleMatcher.matchLocation(pickle.getLocation().getLine());
                        if (pickleLocation != null) {
                            scenarios.put(pickleLocation, feature);
                        }
                    }));
        }
        return scenarios;
    }
}