package courgette.integration.extentreports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.*;
import courgette.runtime.utils.FileUtils;

import java.io.IOException;
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
        final ExtentHtmlReporter extentHtmlReporter = new ExtentHtmlReporter(extentReportsProperties.getReportPath());

        if (extentReportsProperties.getXMLConfigFile() != null) {
            extentHtmlReporter.loadXMLConfig(extentReportsProperties.getXMLConfigFile(), true);
        }

        final ExtentReports extentReports = new ExtentReports();
        extentReports.attachReporter(extentHtmlReporter);

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
                String scenarioName = feature.getScenarios().stream().filter(s -> !s.getName().equals("")).findFirst().get().getName();
                feature.getScenarios().forEach(scenario -> {
                    if (!scenario.getKeyword().equals("Background")) {
                        addScenario(featureNode, scenarioName, scenario.getBefore(), scenario.getAfter(), scenario.getSteps(), scenario.getTags());
                    }
                });
            }
        });
    }

    private void addScenario(ExtentTest featureNode, String scenarioName, List<Hook> before, List<Hook> after, List<Step> steps, List<Tag> tags) {
        final ExtentTest scenarioNode = createGherkinNode(featureNode, "Scenario", scenarioName, false);
        before.forEach(beforeHook -> addImageEmbeddings(scenarioNode, beforeHook));
        steps.forEach(step -> addStep(scenarioNode, step));
        after.forEach(afterHook -> addImageEmbeddings(scenarioNode, afterHook));
        addScenarioTags(scenarioNode, tags);
    }

    private void addScenarioTags(ExtentTest scenarioNode, List<Tag> tags) {
        tags.forEach(tag -> scenarioNode.assignCategory(tag.getName()));
    }

    private void addStep(ExtentTest scenarioNode, Step step) {
        step.getBefore().forEach(beforeStep -> addImageEmbeddings(scenarioNode, beforeStep));
        ExtentTest stepNode = createGherkinNode(scenarioNode, step.getKeyword().trim(), step.getName(), true);
        setResult(stepNode, step.passed(isStrict));
        step.getEmbeddings().forEach(embedding -> embedImage(stepNode, embedding));
        step.getAfter().forEach(afterStep -> addImageEmbeddings(scenarioNode, afterStep));
    }

    private void addImageEmbeddings(ExtentTest scenarioNode, Hook hook) {
        hook.getEmbeddings().forEach(embedding -> embedImage(scenarioNode, embedding));
    }

    private ExtentTest createGherkinNode(ExtentTest parent, String keyword, String name, boolean appendKeyword) {
        try {
            String nodeName = appendKeyword ? (keyword + " " + name) : name;
            return parent.createNode(new GherkinKeyword(keyword), nodeName);
        } catch (ClassNotFoundException e) {
            throw new CourgetteException(e);
        }
    }

    private void embedImage(ExtentTest scenarioNode, Embedding embedding) {
        if (embedding.getMimeType().startsWith("image")) {
            final String imageName = extentReportsProperties.getReportImagesPath() + "/" + embedding.getCourgetteEmbeddingId();
            final String imageFormat = embedding.getMimeType().split("/")[1];
            FileUtils.writeImageFile(imageName, imageFormat, embedding.getData());
            try {
                scenarioNode.addScreenCaptureFromPath("images/" + embedding.getCourgetteEmbeddingId() + "." + imageFormat);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setResult(ExtentTest extentTest, boolean passed) {
        if (!passed) {
            extentTest.fail("");
        }
    }
}