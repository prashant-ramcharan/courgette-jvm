package courgette.runtime;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CourgetteFeatureLoader {
    private final CourgetteProperties courgetteProperties;

    public CourgetteFeatureLoader(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
    }

    public List<CucumberFeature> getCucumberFeatures() {
        final CourgetteRuntimeOptions courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        final List<String> argv = Arrays.asList(courgetteRuntimeOptions.getRuntimeOptions());

        final RuntimeOptions runtimeOptions = new RuntimeOptions(argv);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);

        try {
            return runtimeOptions.cucumberFeatures(resourceLoader);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }
}