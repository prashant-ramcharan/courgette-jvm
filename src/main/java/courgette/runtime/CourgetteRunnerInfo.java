package courgette.runtime;

import courgette.api.CourgetteRunLevel;
import io.cucumber.core.feature.CucumberFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CourgetteRunnerInfo {
    private final CourgetteRuntimeOptions courgetteRuntimeOptions;
    private final Integer lineId;
    private final CourgetteRunLevel courgetteRunLevel;
    private final CucumberFeature cucumberFeature;

    public CourgetteRunnerInfo(CourgetteProperties courgetteProperties, CucumberFeature cucumberFeature, Integer lineId) {
        this.cucumberFeature = cucumberFeature;
        this.courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties, cucumberFeature);
        this.lineId = lineId;
        this.courgetteRunLevel = courgetteProperties.getCourgetteOptions().runLevel();
    }

    public CucumberFeature getCucumberFeature() {
        return cucumberFeature;
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
        return courgetteRuntimeOptions.getReportJsFiles();
    }
}
