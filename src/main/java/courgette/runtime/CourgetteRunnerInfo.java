package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CourgetteRunnerInfo {
    private final CourgetteRuntimeOptions courgetteRuntimeOptions;
    private final Integer lineId;
    private final CourgetteRunLevel courgetteRunLevel;
    private final Feature feature;
    private final boolean rerun;

    public CourgetteRunnerInfo(CourgetteProperties courgetteProperties, Feature feature, Integer lineId) {
        this.feature = feature;
        this.courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties, feature);
        this.lineId = lineId;
        this.courgetteRunLevel = courgetteProperties.getCourgetteOptions().runLevel();
        this.rerun = courgetteProperties.getCourgetteOptions().rerunFailedScenarios() &&
                checkRerunCondition(feature,
                        courgetteProperties.getCourgetteOptions().excludeFeatureFromRerun(),
                        courgetteProperties.getCourgetteOptions().excludeTagFromRerun());
    }

    public Feature getFeature() {
        return feature;
    }

    public Integer getLineId() {
        return lineId;
    }

    public Map<String, List<String>> getRuntimeOptions() {
        Map<String, List<String>> runtimeOptions = courgetteRuntimeOptions.mapRuntimeOptions();

        if (courgetteRunLevel.equals(CourgetteRunLevel.SCENARIO) && lineId != null) {
            final String featurePath = runtimeOptions.get(null).get(0);

            final List<String> scenarioPath = new ArrayList<>();
            scenarioPath.add(String.format("%s:%s", featurePath, lineId));
            runtimeOptions.put(null, scenarioPath);

            runtimeOptions.remove("--tags");
        }
        return runtimeOptions;
    }

    public Map<String, List<String>> getRerunRuntimeOptions(String rerun) {
        final Map<String, List<String>> rerunRuntimeOptions = getRuntimeOptions();
        rerunRuntimeOptions.remove("--tags");

        final List<String> scenarioPath = new ArrayList<>();
        scenarioPath.add(rerun);
        rerunRuntimeOptions.put(null, scenarioPath);

        return rerunRuntimeOptions;
    }

    public String getRerunFile() {
        return courgetteRuntimeOptions.getRerunFile();
    }

    public List<String> getReportFiles() {
        return courgetteRuntimeOptions.getReportFiles();
    }

    public File getJsonReportFile() {
        final String jsonReport = courgetteRuntimeOptions.getJsonReportFile();

        if (jsonReport != null) {
            return new File(jsonReport);
        }
        return null;
    }

    public boolean allowRerun() {
        return rerun;
    }

    private boolean checkRerunCondition(Feature feature, String[] excludedRerunFeatures, String[] excludedRerunTags) {
        return Arrays.stream(excludedRerunFeatures)
                .map(String::trim)
                .map(String::toLowerCase)
                .noneMatch(featureName -> feature.getUri().getPath().toLowerCase().contains(featureName))
                &&
                Arrays.stream(excludedRerunTags)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .noneMatch(featureTag -> feature.getPickles().stream().map(Pickle::getTags)
                                .flatMap(Collection::stream)
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet())
                                .contains(featureTag));

    }
}
