package courgette.runtime;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenarioOutline;

import java.util.*;

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
                if (featureElement instanceof CucumberScenarioOutline) {
                    ((CucumberScenarioOutline) featureElement).getCucumberExamplesList()
                            .forEach(c -> c.getExamples().getRows().stream().skip(1).forEach(row -> {
                                final Integer lineId = row.getLine();

                                runtimeOptions.getFeaturePaths().forEach(resourcePath -> {

                                    final List<String> scenarioPath = new ArrayList<>();
                                    addScenario(scenarioPath, cucumberFeature, resourcePath, lineId);

                                    scenarios.put(
                                            CucumberFeature.load(resourceLoader, scenarioPath, new ArrayList<>()).stream().findFirst().orElse(null),
                                            lineId);
                                });
                            }));
                } else {
                    final Integer lineId = featureElement.getGherkinModel().getLine();

                    runtimeOptions.getFeaturePaths().forEach(resourcePath -> {
                        final List<String> scenarioPath = new ArrayList<>();
                        addScenario(scenarioPath, cucumberFeature, resourcePath, lineId);

                        scenarios.put(
                                CucumberFeature.load(resourceLoader, scenarioPath, new ArrayList<>()).stream().findFirst().orElse(null),
                                lineId);
                    });
                }
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