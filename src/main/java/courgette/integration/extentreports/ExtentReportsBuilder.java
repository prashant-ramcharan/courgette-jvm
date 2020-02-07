package courgette.integration.extentreports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.*;

import java.util.List;
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
            if (feature.getScenarios().size() == 1) {
                Scenario scenario = feature.getScenarios().get(0);
                addScenario(featureNode, scenario.getName(), scenario.getBefore(), scenario.getAfter(), scenario.getSteps(), scenario.getTags());
            } else {
                for (Scenario scenario : feature.getScenarios()) {
                    if (scenario.getKeyword().startsWith("Scenario")) {
                        addScenario(featureNode, scenario.getName(), scenario.getBefore(), scenario.getAfter(), scenario.getSteps(), scenario.getTags());
                    }
                }
            }
        });
    }

    private void addScenario(ExtentTest featureNode, String scenarioName, List<Hook> before, List<Hook> after, List<Step> steps, List<Tag> tags) {
        final ExtentTest scenarioNode = createGherkinNode(featureNode, "Scenario", scenarioName, false);
        addBeforeOrAfterDetails(scenarioNode, before);
        addSteps(scenarioNode, steps);
        addBeforeOrAfterDetails(scenarioNode, after);
        assignCategoryToScenario(scenarioNode, tags);
    }

    private void assignCategoryToScenario(ExtentTest scenarioNode, List<Tag> tags) {
        tags.forEach(tag -> scenarioNode.assignCategory(tag.getName()));
    }

    private void addSteps(ExtentTest scenarioNode, List<Step> steps) {
        steps.forEach(step -> {
            addBeforeOrAfterDetails(scenarioNode, step.getBefore());
            ExtentTest stepNode = createGherkinNode(scenarioNode, step.getKeyword().trim(), step.getName(), true);
            addAdditionalStepDetails(stepNode, step);
            setStepResult(stepNode, step.passed(isStrict));
            addBeforeOrAfterDetails(scenarioNode, step.getAfter());
        });
    }

    private void addBeforeOrAfterDetails(ExtentTest scenarioNode, List<Hook> hooks) {
        hooks.forEach(hook -> {
            addOutputs(scenarioNode, hook.getOutput());
            addError(scenarioNode, hook.getResult().getErrorMessage());
            addImageEmbeddings(scenarioNode, hook.getEmbeddings());
        });
    }

    private void addAdditionalStepDetails(ExtentTest stepNode, Step step) {
        addOutputs(stepNode, step.getOutput());
        addError(stepNode, step.getResult().getErrorMessage());
        addImageEmbeddings(stepNode, step.getEmbeddings());
    }

    private void addOutputs(ExtentTest scenarioNode, List<String> outputs) {
        outputs.forEach(output -> log(scenarioNode, output));
    }

    private void addImageEmbeddings(ExtentTest node, List<Embedding> embeddings) {
        embeddings.forEach(embedding -> {
            if (embedding.getMimeType().startsWith("image")) {
                node.addScreenCaptureFromBase64String(embedding.getData());
            }
        });
    }

    private void addError(ExtentTest scenarioNode, String error) {
        log(scenarioNode, error);
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
}