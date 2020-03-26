package courgette.integration.extentreports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Hook;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.report.model.Step;
import courgette.runtime.report.model.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExtentReportsBuilder {
    private ExtentReportsProperties extentReportsProperties;
    private List<Feature> featureList;
    private boolean isStrict;

    private ExtentReportsBuilder(ExtentReportsProperties extentReportsProperties, List<Feature> featureList, boolean isStrict) {
        this.extentReportsProperties = extentReportsProperties;
        this.featureList = featureList;
        this.isStrict = isStrict;
    }

    public static ExtentReportsBuilder create(ExtentReportsProperties extentReportProperties, List<Feature> featureList, boolean isStrict) {
        return new ExtentReportsBuilder(extentReportProperties, featureList, isStrict);
    }

    public void buildReport() {
        final ExtentSparkReporter extentSparkReporter = new ExtentSparkReporter(extentReportsProperties.getReportFilename());

        if (extentReportsProperties.getXMLConfigFile() != null) {
            extentSparkReporter.loadXMLConfig(extentReportsProperties.getXMLConfigFile(), true);
        }

        final ExtentReports extentReports = new ExtentReports();
        extentReports.setReportUsesManualConfiguration(true);
        extentReports.attachReporter(extentSparkReporter);

        getDistinctFeatureUris().forEach(featureUri -> {
            List<Feature> features = featureList.stream().filter(f -> f.getUri().equals(featureUri)).collect(Collectors.toList());
            addFeatures(extentReports, features);
        });
        extentReports.flush();
    }

    private List<String> getDistinctFeatureUris() {
        return featureList.stream().map(Feature::getUri).distinct().collect(Collectors.toList());
    }

    private void addFeatures(ExtentReports extentReports, List<Feature> features) {
        final ExtentTest featureNode = extentReports.createTest(features.get(0).getName());

        features.forEach(feature -> {
            if (feature.getScenarios().size() > 1) {
                addFeatureStartAndEndTime(featureNode, feature);
            }

            for (Scenario scenario : feature.getScenarios()) {
                if (scenario.getKeyword().startsWith("Scenario")) {
                    addScenario(featureNode, scenario);
                    addScenarioStartAndEndTime(featureNode, scenario);
                }
            }
        });
    }

    private void addScenario(ExtentTest featureNode, Scenario scenario) {
        final ExtentTest scenarioNode = createGherkinNode(featureNode, "Scenario", scenario.getName(), false);
        addBeforeOrAfterDetails(scenarioNode, scenario.getBefore());
        addSteps(scenarioNode, scenario);
        addBeforeOrAfterDetails(scenarioNode, scenario.getAfter());
        assignCategoryToScenario(scenarioNode, scenario.getTags());
    }

    private void assignCategoryToScenario(ExtentTest scenarioNode, List<Tag> tags) {
        tags.forEach(tag -> scenarioNode.assignCategory(tag.getName()));
    }

    private void addSteps(ExtentTest scenarioNode, Scenario scenario) {
        Date startTime = getStartTime(scenario.getStartTimestamp());
        List<Step> steps = scenario.getSteps();

        steps.forEach(step -> {
            addBeforeOrAfterDetails(scenarioNode, step.getBefore());
            ExtentTest stepNode = createStepNode(scenarioNode, step, startTime);
            addAdditionalStepDetails(stepNode, step);
            setStepResult(stepNode, step.passed(isStrict));
            addBeforeOrAfterDetails(scenarioNode, step.getAfter());
        });
    }

    private ExtentTest createStepNode(ExtentTest scenarioNode, Step step, Date startTime) {
        ExtentTest stepNode = createGherkinNode(scenarioNode, step.getKeyword().trim(), step.getName(), true);
        stepNode.getModel().setStartTime(startTime);
        return stepNode;
    }

    private void addBeforeOrAfterDetails(ExtentTest scenarioNode, List<Hook> hooks) {
        hooks.forEach(hook -> {
            String error = hook.getResult().getErrorMessage();
            List<String> output = hook.getOutput();
            List<Embedding> embeddings = hook.getEmbeddings();

            addOutputs(scenarioNode, output);
            addError(scenarioNode, error);
            addImageEmbeddings(scenarioNode, embeddings);
        });
    }

    private void addAdditionalStepDetails(ExtentTest stepNode, Step step) {
        String error = step.getResult().getErrorMessage();
        List<String> output = step.getOutput();
        List<Embedding> embeddings = step.getEmbeddings();

        addOutputs(stepNode, output);
        addError(stepNode, error);
        addImageEmbeddings(stepNode, embeddings);
    }

    private void addOutputs(ExtentTest node, List<String> outputs) {
        outputs.forEach(output -> log(node, output));
    }

    private void addImageEmbeddings(ExtentTest node, List<Embedding> embeddings) {
        embeddings.forEach(embedding -> {
            if (embedding.getMimeType().startsWith("image")) {
                node.addScreenCaptureFromBase64String(embedding.getData());
            }
        });
    }

    private void addError(ExtentTest node, String error) {
        log(node, error);
    }

    private void log(ExtentTest node, String message) {
        if (message != null) {
            node.log(Status.INFO, message);
        }
    }

    private ExtentTest createGherkinNode(ExtentTest parent, String keyword, String name, boolean appendKeyword) {
        try {
            String nodeName = appendKeyword ? (keyword + " " + name) : name;
            return parent.createNode(new GherkinKeyword(keyword), nodeName);
        } catch (ClassNotFoundException e) {
            throw new CourgetteException(e);
        }
    }

    private void setStepResult(ExtentTest extentTest, boolean passed) {
        if (!passed) {
            extentTest.fail("Step failed");
        }
    }

    private void addFeatureStartAndEndTime(ExtentTest featureNode, Feature feature) {
        Date featureStartTime = getEarliestStartTime(feature.getScenarios());
        Date featureEndTime = getEndTime(featureStartTime.getTime(), feature.getScenarios());
        addStartAndEndTime(featureNode, featureStartTime, featureEndTime);
    }

    private void addScenarioStartAndEndTime(ExtentTest scenarioNode, Scenario scenario) {
        List<Scenario> scenarios = new ArrayList<>();
        scenarios.add(scenario);
        Date scenarioStartTime = getStartTime(scenario.getStartTimestamp());
        Date scenarioEndTime = getEndTime(scenarioStartTime.getTime(), scenarios);
        addStartAndEndTime(scenarioNode, scenarioStartTime, scenarioEndTime);
    }

    private void addStartAndEndTime(ExtentTest node, Date startTime, Date endTime) {
        node.getModel().setStartTime(startTime);
        node.getModel().setEndTime(endTime);
    }

    private Date getEarliestStartTime(List<Scenario> scenarios) {
        List<Long> times = new ArrayList<>();
        scenarios.stream().map(Scenario::getStartTimestamp).filter(time -> time.length() > 0).forEach(time -> times.add(Date.from(Instant.parse(time)).getTime()));
        return new Date(times.stream().reduce((start, end) -> start).get());
    }

    private Date getStartTime(String timestamp) {
        return Date.from(Instant.parse(timestamp));
    }

    private Date getEndTime(long startTime, List<Scenario> scenarios) {
        long endTime = 0;

        for (Scenario scenario : scenarios) {
            endTime = endTime + calculateDuration.apply(scenario.getBefore().stream().map(Hook::getResult).collect(Collectors.toList()));
            endTime = endTime + calculateDuration.apply(scenario.getAfter().stream().map(Hook::getResult).collect(Collectors.toList()));
            endTime = endTime + calculateDuration.apply(scenario.getSteps().stream().map(Step::getResult).collect(Collectors.toList()));
            endTime = endTime + calculateDuration.apply(scenario.getSteps().stream().flatMap(s -> s.getBefore().stream()).map(Hook::getResult).collect(Collectors.toList()));
            endTime = endTime + calculateDuration.apply(scenario.getSteps().stream().flatMap(s -> s.getAfter().stream()).map(Hook::getResult).collect(Collectors.toList()));
        }
        return new Date(startTime + endTime);
    }

    private Function<List<Result>, Long> calculateDuration = (source) -> source.stream().mapToLong(Result::getDuration).sum();
}