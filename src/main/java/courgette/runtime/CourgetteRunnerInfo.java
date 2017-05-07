package courgette.runtime;

import courgette.api.RunScope;
import cucumber.runtime.model.CucumberFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CourgetteRunnerInfo {
    private final CourgetteProperties courgetteProperties;
    private CourgetteRuntimeOptions courgetteRuntimeOptions;
    private final CucumberFeature cucumberFeature;
    private final Integer lineId;
    private final RunScope runScope;

    public CourgetteRunnerInfo(CourgetteProperties courgetteProperties, CucumberFeature cucumberFeature, Integer lineId) {
        this.courgetteProperties = courgetteProperties;
        this.cucumberFeature = cucumberFeature;
        this.courgetteRuntimeOptions = new CourgetteRuntimeOptions(courgetteProperties, cucumberFeature);
        this.lineId = lineId;
        this.runScope = courgetteProperties.getCourgetteOptions().runScope();
    }

    public CucumberFeature getCucumberFeature() {
        return cucumberFeature;
    }

    public Map<String, List<String>> getRuntimeOptions() {
        Map<String, List<String>> runtimeOptions = courgetteRuntimeOptions.mapRuntimeOptions();

        if (runScope.equals(RunScope.SCENARIO_SCOPE) && lineId != null) {
            final String featurePath = runtimeOptions.get(null).get(0);

            final List<String> scenarioPath = new ArrayList<>();
            scenarioPath.add(String.format("%s:%s", featurePath, lineId));

            runtimeOptions.put(null, scenarioPath);
            runtimeOptions.remove("--tags");
        }
        return runtimeOptions;
    }

    public Map<String, List<String>> getReruntimeOptions(String rerun) {
        return new CourgetteRuntimeOptions(courgetteProperties, cucumberFeature).mapReruntimeOptions(rerun);
    }

    public String getRerunFile() {
        return courgetteRuntimeOptions.getRerunFile();
    }

    public List<String> getReportFiles() {
        return courgetteRuntimeOptions.getReportJsFiles();
    }

    public Integer getLineId() {
        return lineId;
    }

    public RunScope getRunScope() {
        return runScope;
    }
}
