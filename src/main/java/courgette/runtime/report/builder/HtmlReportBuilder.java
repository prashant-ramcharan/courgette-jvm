package courgette.runtime.report.builder;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import courgette.runtime.CourgetteEnvironmentInfo;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Hook;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.report.model.Step;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

public class HtmlReportBuilder {

    private static final String PASSED = "Passed";
    private static final String PASSED_AFTER_RERUN = "Passed after Rerun";
    private static final String FAILED = "Failed";
    private static final String FAILED_AFTER_RERUN = "Failed after Rerun";
    private static final String SUCCESS = "success";
    private static final String DANGER = "danger";
    private static final String WARNING = "warning";
    private static final String DATA_TARGET = "data_target";
    private static final String FEATURE_NAME = "feature_name";
    private static final String FEATURE_BADGE = "feature_badge";
    private static final String FEATURE_RESULT = "feature_result";
    private static final String FEATURE_SCENARIOS = "feature_scenarios";
    private static final String SCENARIO_NAME = "scenario_name";
    private static final String SCENARIO_BADGE = "scenario_badge";
    private static final String SCENARIO_RESULT = "scenario_result";
    private static final String SCENARIO_TAGS = "scenario_tags";
    private static final String MODAL_TARGET = "modal_target";
    private static final String MODAL_HEADING = "modal_heading";
    private static final String MODAL_FEATURE_LINE = "modal_feature_line";
    private static final String MODAL_BODY = "modal_body";
    private static final String STEP_KEYWORD = "step_keyword";
    private static final String STEP_NAME = "step_name";
    private static final String STEP_DURATION = "step_duration";
    private static final String STEP_BADGE = "step_badge";
    private static final String STEP_RESULT = "step_result";
    private static final String STEP_DATATABLE = "step_datatable";
    private static final String DATATABLE = "datatable";
    private static final String STEP_EXCEPTION = "step_exception";
    private static final String EXCEPTION = "exception";
    private static final String STEP_OUTPUT_BEFORE = "step_output_before";
    private static final String STEP_OUTPUT = "step_output";
    private static final String STEP_OUTPUT_AFTER = "step_output_after";
    private static final String OUTPUT = "output";
    private static final String STEP_EMBEDDING_TEXT_BEFORE = "step_embedding_text_before";
    private static final String STEP_EMBEDDING_TEXT = "step_embedding_text";
    private static final String STEP_EMBEDDING_TEXT_AFTER = "step_embedding_text_after";
    private static final String TEXT = "text";
    private static final String STEP_EMBEDDING_IMAGE_BEFORE = "step_embedding_image_before";
    private static final String STEP_EMBEDDING_IMAGE = "step_embedding_image";
    private static final String STEP_EMBEDDING_IMAGE_AFTER = "step_embedding_image_after";
    private static final String IMAGE_ID = "img_id";
    private static final String ROW_INFO = "row_info";
    private static final String TAG = "tag";

    private List<Feature> featureList;
    private List<CourgetteRunResult> courgetteRunResults;
    private CourgetteProperties courgetteProperties;

    private Mustache featureTemplate;
    private Mustache modalTemplate;
    private Mustache modalStepTemplate;
    private Mustache modalEnvironmentTemplate;
    private Mustache modalRowTemplate;
    private Mustache scenarioTemplate;
    private Mustache scenarioTagTemplate;

    private HtmlReportBuilder(List<Feature> featureList,
                              List<CourgetteRunResult> courgetteRunResults,
                              CourgetteProperties courgetteProperties) {

        this.featureList = featureList;
        this.courgetteRunResults = courgetteRunResults;
        this.courgetteProperties = courgetteProperties;

        this.featureTemplate = readTemplate("/report/templates/feature.mustache");
        this.modalTemplate = readTemplate("/report/templates/modal.mustache");
        this.modalStepTemplate = readTemplate("/report/templates/modal_step.mustache");
        this.modalEnvironmentTemplate = readTemplate("/report/templates/modal_environment.mustache");
        this.modalRowTemplate = readTemplate("/report/templates/modal_row.mustache");
        this.scenarioTemplate = readTemplate("/report/templates/scenario.mustache");
        this.scenarioTagTemplate = readTemplate("/report/templates/scenario_tag.mustache");
    }

    public static HtmlReportBuilder create(List<Feature> featureList,
                                           List<CourgetteRunResult> courgetteRunResults,
                                           CourgetteProperties courgetteProperties) {

        return new HtmlReportBuilder(featureList, courgetteRunResults, courgetteProperties);
    }

    public List<String> getHtmlTableFeatureRows() {
        final List<String> featureRows = new ArrayList<>(featureList.size());
        featureList.forEach(feature -> featureRows.add(createFeatureRow(feature)));
        return featureRows;
    }

    public List<String> getHtmlModals() {
        final int modalCapacity = (int) featureList.stream().map(Feature::getScenarios).count() + 1;

        final List<String> modals = new ArrayList<>(modalCapacity);

        modals.add(createEnvironmentInfoModal());

        featureList
                .forEach(feature -> {
                    List<Scenario> scenarios = feature.getScenarios();
                    scenarios.forEach(scenario -> modals.add(createScenarioModal(feature, scenario)));
                });

        return modals;
    }

    private String createFeatureRow(Feature feature) {
        final LinkedHashMap<String, Object> featureData = new LinkedHashMap<>();

        String featureId = feature.getCourgetteFeatureId();
        String featureName = feature.getName();
        String featureBadge = feature.passed() ? SUCCESS : DANGER;
        String featureResult = featureBadge.equals(SUCCESS) ? PASSED : FAILED;

        LinkedList<String> scenarioRows = new LinkedList<>();
        createScenarios(feature, scenarioRows);

        featureData.put(DATA_TARGET, featureId);
        featureData.put(FEATURE_NAME, featureName);
        featureData.put(FEATURE_BADGE, featureBadge);
        featureData.put(FEATURE_RESULT, featureResult);
        featureData.put(FEATURE_SCENARIOS, scenarioRows);

        return createFromTemplate(featureTemplate, featureData);
    }

    private void createScenarios(Feature feature, LinkedList<String> scenarioRows) {
        feature.getScenarios().forEach(scenario -> {
            if (!scenario.getKeyword().equalsIgnoreCase("Background")) {
                scenarioRows.add(createScenarioRow(feature.getCourgetteFeatureId(), scenario));
            }
        });
    }

    private List<String> createScenarioTags(Scenario scenario) {

        final LinkedList<String> scenarioTags = new LinkedList<>();

        scenario.getTags().forEach(tag -> {
            final LinkedHashMap<String, Object> scenarioTagData = new LinkedHashMap<>();
            scenarioTagData.put(TAG, tag.getName());
            scenarioTags.add(createFromTemplate(scenarioTagTemplate, scenarioTagData));
        });

        return scenarioTags;
    }

    private String createScenarioRow(String featureId, Scenario scenario) {

        final LinkedHashMap<String, Object> scenarioData = new LinkedHashMap<>();

        String scenarioId = scenario.getCourgetteScenarioId();
        String scenarioName = scenario.getName();
        String scenarioBadge = scenario.passed() ? SUCCESS : DANGER;
        String scenarioResult = scenarioBadge.equals(SUCCESS) ? PASSED : FAILED;

        List<CourgetteRunResult> scenarioRunResult = courgetteRunResults
                .stream()
                .filter(result -> {
                    String featureUri = courgetteProperties.isFeatureRunLevel() ?
                            scenario.getFeatureUri() :
                            (scenario.getFeatureUri() + ":" + scenario.getLine());
                    return result.getFeatureUri().endsWith(featureUri.split("file:")[1]);
                })
                .collect(Collectors.toList());

        switch (scenarioBadge) {
            case DANGER:
                if (scenarioRunResult.stream().anyMatch(result -> result.getStatus() == CourgetteRunResult.Status.FAILED_AFTER_RERUN)) {
                    scenarioResult = FAILED_AFTER_RERUN;
                }
                break;

            case SUCCESS:
                if (scenarioRunResult.stream().anyMatch(result -> result.getStatus() == CourgetteRunResult.Status.PASSED_AFTER_RERUN)) {
                    scenarioResult = PASSED_AFTER_RERUN;
                }
                break;
        }

        scenarioData.put(DATA_TARGET, featureId);
        scenarioData.put(MODAL_TARGET, scenarioId);
        scenarioData.put(SCENARIO_NAME, scenarioName);
        scenarioData.put(SCENARIO_BADGE, scenarioBadge);
        scenarioData.put(SCENARIO_RESULT, scenarioResult);
        scenarioData.put(SCENARIO_TAGS, createScenarioTags(scenario));

        return createFromTemplate(scenarioTemplate, scenarioData);
    }

    private String createScenarioModal(Feature feature, Scenario scenario) {
        final String featureName = feature.getUri().substring(feature.getUri().lastIndexOf("/") + 1);

        final LinkedHashMap<String, Object> modalData = new LinkedHashMap();

        modalData.put(MODAL_TARGET, scenario.getCourgetteScenarioId());
        modalData.put(MODAL_HEADING, scenario.getName());
        modalData.put(MODAL_FEATURE_LINE, featureName + " - line " + scenario.getLine());

        List<String> modalBody = new ArrayList<>();

        scenario.getBefore().forEach(hook -> modalBody.add(createRowFromHook(hook)));
        scenario.getSteps().forEach(step -> modalBody.add(createRowFromStep(step)));
        scenario.getAfter().forEach(hook -> modalBody.add(createRowFromHook(hook)));

        modalData.put(MODAL_BODY, modalBody);

        return createFromTemplate(modalTemplate, modalData);
    }

    private String createRowFromHook(Hook hook) {

        final LinkedHashMap<String, Object> hookData = new LinkedHashMap<>();

        String stepStatusBadge = statusBadge.apply(hook.getResult());

        String stepResult = statusLabel.apply(hook.getResult());

        hookData.put(STEP_NAME, hook.getLocation());
        hookData.put(STEP_DURATION, hook.getResult().getDuration());
        hookData.put(STEP_BADGE, stepStatusBadge);
        hookData.put(STEP_RESULT, stepResult);

        if (hook.getResult().getErrorMessage() != null) {
            addNestedMap(hookData, STEP_EXCEPTION, EXCEPTION, hook.getResult().getErrorMessage());
        }

        if (!hook.getOutput().isEmpty()) {
            addNestedMap(hookData, STEP_OUTPUT, OUTPUT, hook.getOutput());
        }

        hook.getEmbeddings().forEach(embedding -> {

            if (embedding.getMimeType().equals("text/html")) {
                String htmlData = new String(Base64.getDecoder().decode(embedding.getData()));
                addNestedMap(hookData, STEP_EMBEDDING_TEXT, TEXT, htmlData);

            } else if (embedding.getMimeType().startsWith("image")) {
                addNestedMap(hookData, STEP_EMBEDDING_IMAGE, IMAGE_ID, embedding.getCourgetteEmbeddingId());
            }
        });

        return createFromTemplate(modalStepTemplate, hookData);
    }

    private String createRowFromStep(Step step) {
        final LinkedHashMap<String, Object> stepData = new LinkedHashMap<>();

        String stepStatusBadge = statusBadge.apply(step.getResult());

        String stepResult = statusLabel.apply(step.getResult());

        stepData.put(STEP_KEYWORD, step.getKeyword());
        stepData.put(STEP_NAME, step.getName());
        stepData.put(STEP_DURATION, step.getResult().getDuration());
        stepData.put(STEP_BADGE, stepStatusBadge);
        stepData.put(STEP_RESULT, stepResult);

        if (step.getRowData() != null) {
            addNestedMap(stepData, STEP_DATATABLE, DATATABLE, step.getRowData());
        }

        if (step.getResult().getErrorMessage() != null) {
            addNestedMap(stepData, STEP_EXCEPTION, EXCEPTION, step.getResult().getErrorMessage());
        }

        addStepOutput(stepData, step);
        addStepImageEmbedding(stepData, step);
        addStepTextHtmlEmbedding(stepData, step);

        return createFromTemplate(modalStepTemplate, stepData);
    }

    private String createModalRow(String rowInfo) {

        final LinkedHashMap<String, Object> rowInfoData = new LinkedHashMap();
        rowInfoData.put(ROW_INFO, rowInfo);

        return createFromTemplate(modalRowTemplate, rowInfoData);
    }

    private String createEnvironmentInfoModal() {
        String environmentInfo = courgetteProperties.getCourgetteOptions().environmentInfo();

        List<String> environment = new CourgetteEnvironmentInfo(environmentInfo).list();
        if (environment.isEmpty()) {
            environment.add("No additional environment information provided.");
        }

        LinkedHashMap<String, Object> environmentInfoData = new LinkedHashMap<>();
        LinkedList<String> rowInfo = new LinkedList<>();
        environment.forEach(info -> rowInfo.add(createModalRow(info)));

        environmentInfoData.put(MODAL_BODY, rowInfo);

        return createFromTemplate(modalEnvironmentTemplate, environmentInfoData);
    }

    private static Function<Result, String> statusLabel = (result) -> result.getStatus().substring(0, 1).toUpperCase() + result.getStatus().substring(1);

    private static Function<Result, String> statusBadge = (result) -> {
        String status = result.getStatus();
        return status.equalsIgnoreCase(PASSED) ? SUCCESS : status.equalsIgnoreCase(FAILED) ? DANGER : WARNING;
    };

    private String createFromTemplate(Mustache template, Object data) {
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return writer.toString();
    }

    private static void addNestedMap(HashMap<String, Object> source, String sourceKey,
                                     String childKey, Object childValue) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(childKey, childValue);
        source.put(sourceKey, map);
    }

    private void addStepOutput(HashMap<String, Object> source, Step step) {
        List<String> beforeStep = step.getBefore().stream().flatMap(o -> o.getOutput().stream()).collect(Collectors.toList());
        List<String> currentStep = step.getOutput();
        List<String> afterStep = step.getAfter().stream().flatMap(o -> o.getOutput().stream()).collect(Collectors.toList());

        addNonEmptyNestedMap(source, STEP_OUTPUT_BEFORE, OUTPUT, beforeStep);
        addNonEmptyNestedMap(source, STEP_OUTPUT, OUTPUT, currentStep);
        addNonEmptyNestedMap(source, STEP_OUTPUT_AFTER, OUTPUT, afterStep);
    }

    private void addStepImageEmbedding(HashMap<String, Object> source, Step step) {
        List<String> beforeStep = step.getBefore().stream()
                .flatMap(e -> e.getEmbeddings().stream())
                .filter(imageFilter)
                .map(Embedding::getCourgetteEmbeddingId)
                .collect(Collectors.toList());

        List<String> currentStep = step.getEmbeddings().stream()
                .filter(imageFilter)
                .map(Embedding::getCourgetteEmbeddingId)
                .collect(Collectors.toList());

        List<String> afterStep = step.getAfter().stream()
                .flatMap(e -> e.getEmbeddings().stream())
                .filter(imageFilter)
                .map(Embedding::getCourgetteEmbeddingId)
                .collect(Collectors.toList());

        addNonEmptyNestedMap(source, STEP_EMBEDDING_IMAGE_BEFORE, IMAGE_ID, beforeStep);
        addNonEmptyNestedMap(source, STEP_EMBEDDING_IMAGE, IMAGE_ID, currentStep);
        addNonEmptyNestedMap(source, STEP_EMBEDDING_IMAGE_AFTER, IMAGE_ID, afterStep);
    }

    private void addStepTextHtmlEmbedding(HashMap<String, Object> source, Step step) {
        List<String> beforeStep = step.getBefore().stream()
                .flatMap(e -> e.getEmbeddings().stream())
                .filter(textHtmlFilter)
                .map(embedding -> new String(Base64.getDecoder().decode(embedding.getData())))
                .collect(Collectors.toList());

        List<String> currentStep = step.getEmbeddings().stream()
                .filter(textHtmlFilter)
                .map(embedding -> new String(Base64.getDecoder().decode(embedding.getData())))
                .collect(Collectors.toList());

        List<String> afterStep = step.getAfter().stream()
                .flatMap(e -> e.getEmbeddings().stream())
                .filter(textHtmlFilter)
                .map(embedding -> new String(Base64.getDecoder().decode(embedding.getData())))
                .collect(Collectors.toList());

        addNonEmptyNestedMap(source, STEP_EMBEDDING_TEXT_BEFORE, TEXT, beforeStep);
        addNonEmptyNestedMap(source, STEP_EMBEDDING_TEXT, TEXT, currentStep);
        addNonEmptyNestedMap(source, STEP_EMBEDDING_TEXT_AFTER, TEXT, afterStep);
    }

    private void addNonEmptyNestedMap(HashMap<String, Object> source, String sourceKey, String childKey, List<String> data) {
        if (!data.isEmpty()) {
            addNestedMap(source, sourceKey, childKey, data);
        }
    }

    private Mustache readTemplate(String template) {
        StringBuilder templateContent = new StringBuilder();

        try {
            final InputStream in = getClass().getResourceAsStream(template);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                templateContent.append(line);
            }
        } catch (Exception e) {
            printExceptionStackTrace(e);
        }

        return new DefaultMustacheFactory().compile(new StringReader(templateContent.toString()), "");
    }

    private final Predicate<Embedding> imageFilter = (e) -> e.getMimeType().startsWith("image");
    private final Predicate<Embedding> textHtmlFilter = (e) -> e.getMimeType().equals("text/html");
}
