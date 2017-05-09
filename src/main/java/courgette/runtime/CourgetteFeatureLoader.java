package courgette.runtime;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenarioOutline;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CourgetteFeatureLoader {
    private final CourgetteProperties courgetteProperties;
    private final ClassLoader classLoader;
    private final ResourceLoader resourceLoader;

    private RuntimeOptions runtimeOptions;
    private List<CucumberFeature> cucumberFeatures;

    public CourgetteFeatureLoader(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.resourceLoader = new MultiLoader(classLoader);
    }

    public List<CucumberFeature> getCucumberFeatures() {
        final CourgetteRuntimeOptions courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        final List<String> argv = Arrays.asList(courgetteRuntimeOptions.getRuntimeOptions());

        try {
            runtimeOptions = new RuntimeOptions(argv);
            cucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader);
            return cucumberFeatures;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public Map<CucumberFeature, Integer> getCucumberScenarios() {
        return cucumberScenarios(cucumberFeatures, runtimeOptions);
    }

    public RuntimeOptions getCucumberRuntimeOptions() {
        final CourgetteRuntimeOptions courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties);
        return new RuntimeOptions(Arrays.asList(courgetteRuntimeOptions.getRuntimeOptions()));
    }

    private Map<CucumberFeature, Integer> cucumberScenarios(List<CucumberFeature> cucumberFeatures, RuntimeOptions runtimeOptions) {
        final Map<CucumberFeature, Integer> scenarios = new HashMap<>();

        if (cucumberFeatures != null) {
            cucumberFeatures.forEach(cucumberFeature -> cucumberFeature.getFeatureElements().forEach(featureElement -> {

                final List<Integer> lineIds = new ArrayList<>();

                if (featureElement instanceof CucumberScenarioOutline) {
                    ((CucumberScenarioOutline) featureElement).getCucumberExamplesList()
                            .forEach(c -> c.getExamples().getRows().stream().skip(1).forEach(row -> {
                                lineIds.add(row.getLine());
                            }));
                } else {
                    lineIds.add(featureElement.getGherkinModel().getLine());
                }

                lineIds.forEach(lineId -> {
                    final AtomicBoolean alreadyAdded = new AtomicBoolean(Boolean.FALSE);

                    runtimeOptions.getFeaturePaths().forEach(resourcePath -> {
                        if (!alreadyAdded.get()) {
                            final List<String> scenarioPath = new ArrayList<>();
                            addScenario(scenarioPath, cucumberFeature, resourcePath, lineId);

                            try {
                                scenarios.put(
                                        CucumberFeature.load(resourceLoader, scenarioPath, new ArrayList<>()).stream().findFirst().orElse(null),
                                        lineId);
                                alreadyAdded.set(Boolean.TRUE);
                            } catch (IllegalArgumentException e) {
                                alreadyAdded.set(Boolean.FALSE);
                            }
                        }
                    });
                });
            }));
        }
        return scenarios;
    }

    private void addScenario(List<String> path, CucumberFeature cucumberFeature, String resourcePath, Integer lineId) {
        if (cucumberFeature.getPath().startsWith(resourcePath)) {
            path.add(String.format("%s:%s", cucumberFeature.getPath(), lineId));
        } else {
            path.add(String.format("%s/%s:%s", resourcePath, cucumberFeature.getPath(), lineId));
        }
    }
}