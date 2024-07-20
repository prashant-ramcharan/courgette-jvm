package courgette.runtime.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.Embedding;
import courgette.runtime.report.model.Feature;
import courgette.runtime.report.model.Hook;
import courgette.runtime.report.model.Result;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.report.model.Step;
import courgette.runtime.report.model.Tag;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonReportParser {
    private final String jsonFile;
    private final Boolean isFeatureRunLevel;
    private final List<Feature> features = new ArrayList<>();
    private final static String START_TIMESTAMP_ATTRIBUTE = "start_timestamp";
    private final static String NAME_ATTRIBUTE = "name";
    private final static String URI_ATTRIBUTE = "uri";
    private final static String ELEMENTS_ATTRIBUTE = "elements";
    private final static String STEPS_ATTRIBUTE = "steps";
    private final static String KEYWORD_ATTRIBUTE = "keyword";
    private final static String LINE_ATTRIBUTE = "line";
    private final static String RESULT_ATTRIBUTE = "result";
    private final static String STATUS_ATTRIBUTE = "status";
    private final static String DURATION_ATTRIBUTE = "duration";
    private final static String ERROR_MESSAGE_ATTRIBUTE = "error_message";
    private final static String BEFORE_ATTRIBUTE = "before";
    private final static String AFTER_ATTRIBUTE = "after";
    private final static String MATCH_ATTRIBUTE = "match";
    private final static String LOCATION_ATTRIBUTE = "location";
    private final static String EMBEDDINGS_ATTRIBUTE = "embeddings";
    private final static String OUTPUT_ATTRIBUTE = "output";
    private final static String DATA_ATTRIBUTE = "data";
    private final static String MIME_TYPE_ATTRIBUTE = "mime_type";
    private final static String ROWS_ATTRIBUTE = "rows";
    private final static String CELLS_ATTRIBUTE = "cells";
    private final static String TAGS_ATTRIBUTE = "tags";

    public JsonReportParser(String jsonFile, boolean isFeatureRunLevel) {
        this.jsonFile = jsonFile;
        this.isFeatureRunLevel = isFeatureRunLevel;
    }

    public void createFeatures() {
        createFeatures(jsonFile, isFeatureRunLevel);
    }

    public List<Feature> getFeatures() {
        return features;
    }

    private void createFeatures(String jsonFile, boolean isFeatureRunLevel) {
        try {
            parseJsonReport(jsonFile);
        } catch (FileNotFoundException | IllegalStateException e) {
            throw new CourgetteException(e);
        }
        if (!isFeatureRunLevel) {
            convertToFeatureList(features);
        }
    }

    private void convertToFeatureList(List<Feature> features) {
        Map<String, List<Scenario>> scenarioMap = new HashMap<>();

        features.forEach(feature -> {
            final String uri = feature.getUri();

            if (scenarioMap.containsKey(uri)) {
                List<Scenario> scenarioList = scenarioMap.get(uri);
                scenarioList.addAll(feature.getScenarios());
                scenarioList.sort(Comparator.comparingInt(Scenario::getLine));
                scenarioMap.put(uri, scenarioList);
            } else {
                scenarioMap.put(uri, feature.getScenarios());
            }
        });

        List<Feature> featureList = new ArrayList<>();

        scenarioMap.forEach((key, value) -> {
            Feature feature = features.stream().filter(f -> f.getUri().equals(key)).findFirst().get();
            featureList.add(new Feature(feature.getName(), feature.getUri(), value));
        });

        features.clear();
        features.addAll(featureList);
    }

    private void parseJsonReport(String jsonFile) throws FileNotFoundException {
        JsonParser jsonParser = new JsonParser();
        Object json = jsonParser.parse(new FileReader(jsonFile));

        if (json instanceof JsonNull) {
            return;
        }

        JsonArray reportJson = (JsonArray) json;

        for (JsonElement report : reportJson) {
            final JsonObject feature = report.getAsJsonObject();

            String featureName = feature.get(NAME_ATTRIBUTE).getAsString();
            String featureUri = feature.get(URI_ATTRIBUTE).getAsString();

            JsonArray elements = (JsonArray) feature.get(ELEMENTS_ATTRIBUTE);
            Iterator<JsonElement> elementsIterator = elements.iterator();

            final List<JsonArray> backgroundSteps = new ArrayList<>();

            while (elementsIterator.hasNext()) {
                JsonObject element = elementsIterator.next().getAsJsonObject();

                if (element.get(KEYWORD_ATTRIBUTE).getAsString().equalsIgnoreCase("Background")) {
                    JsonArray elementSteps = (JsonArray) element.get(STEPS_ATTRIBUTE);
                    backgroundSteps.add(elementSteps);
                }
            }

            elementsIterator = elements.iterator();

            final List<Scenario> scenarioElements = new ArrayList<>();

            int index = 0;
            while (elementsIterator.hasNext()) {
                JsonObject scenario = elementsIterator.next().getAsJsonObject();

                JsonElement startTimestampElement = scenario.get(START_TIMESTAMP_ATTRIBUTE);
                String startTimestamp = startTimestampElement != null ? startTimestampElement.getAsString() : "";
                String scenarioName = scenario.get(NAME_ATTRIBUTE).getAsString();
                String scenarioKeyword = scenario.get(KEYWORD_ATTRIBUTE).getAsString();
                int scenarioLine = scenario.get(LINE_ATTRIBUTE).getAsInt();

                if (scenarioKeyword.equalsIgnoreCase("Background")) {
                    continue;
                }

                final List<Hook> scenarioBefore = new ArrayList<>();
                addHook(scenario.get(BEFORE_ATTRIBUTE), scenarioBefore);

                final List<Hook> scenarioAfter = new ArrayList<>();
                addHook(scenario.get(AFTER_ATTRIBUTE), scenarioAfter);

                List<JsonArray> allSteps = new ArrayList<>();
                if (!backgroundSteps.isEmpty()) {
                    allSteps.add(backgroundSteps.get(index++));
                }

                allSteps.addAll(Collections.singleton((JsonArray) scenario.get(STEPS_ATTRIBUTE)));

                final List<Step> scenarioSteps = new ArrayList<>();
                allSteps.forEach(steps -> addSteps(steps, scenarioSteps));

                final List<Tag> scenarioTags = new ArrayList<>();
                addTags(scenario.get(TAGS_ATTRIBUTE), scenarioTags);

                scenarioElements.add(new Scenario(featureUri, startTimestamp, scenarioName, scenarioKeyword, scenarioLine, scenarioBefore, scenarioAfter, scenarioSteps, scenarioTags));
            }
            features.add(new Feature(featureName, featureUri, scenarioElements));
        }
    }

    private void addTags(JsonElement tags, List<Tag> tagList) {
        if (tags != null && tags.getAsJsonArray() != null) {
            tags.getAsJsonArray().forEach(tag -> {
                if (tag.getAsJsonObject() != null) {
                    tagList.add(new Tag(tag.getAsJsonObject().get(NAME_ATTRIBUTE).getAsString()));
                }
            });
        }
    }

    private void addSteps(JsonArray steps, List<Step> stepList) {
        steps.iterator().forEachRemaining(e -> {
            JsonObject step = e.getAsJsonObject();
            JsonObject result = step.get(RESULT_ATTRIBUTE).getAsJsonObject();

            String stepName = step.get(NAME_ATTRIBUTE).getAsString();
            String stepKeyword = step.get(KEYWORD_ATTRIBUTE).getAsString();
            String stepStatus = result.get(STATUS_ATTRIBUTE).getAsString();
            long stepDuration = result.get(DURATION_ATTRIBUTE) != null ? result.get(DURATION_ATTRIBUTE).getAsLong() : 0L;
            String stepErrorMessage = result.get(ERROR_MESSAGE_ATTRIBUTE) != null ? result.get(ERROR_MESSAGE_ATTRIBUTE).getAsString() : null;

            Result stepResult = new Result(stepStatus, stepDuration, stepErrorMessage);

            final List<Hook> stepBefore = new ArrayList<>();
            addHook(step.get(BEFORE_ATTRIBUTE), stepBefore);

            final List<Hook> stepAfter = new ArrayList<>();
            addHook(step.get(AFTER_ATTRIBUTE), stepAfter);

            final List<Embedding> stepEmbeddings = new ArrayList<>();
            addEmbeddings(step, stepEmbeddings);

            final List<String> stepOutputs = new ArrayList<>();
            addOutputs(step, stepOutputs);

            final List<String> stepRowData = new ArrayList<>();
            addStepRowData(step, stepRowData);

            stepList.add(new Step(stepName, stepKeyword, stepResult, stepBefore, stepAfter, stepEmbeddings, stepOutputs, stepRowData));
        });
    }

    private void addHook(JsonElement source, List<Hook> hooks) {
        if (source != null) {
            for (JsonElement scenarioHook : (JsonArray) source) {
                JsonObject hook = scenarioHook.getAsJsonObject();

                JsonObject hookResult = hook.get(RESULT_ATTRIBUTE).getAsJsonObject();
                String hookStatus = hookResult.get(STATUS_ATTRIBUTE).getAsString();
                long hookDuration = hookResult.get(DURATION_ATTRIBUTE) != null ? hookResult.get(DURATION_ATTRIBUTE).getAsLong() : 0L;
                String hookErrorMessage = hookResult.get(ERROR_MESSAGE_ATTRIBUTE) != null ? hookResult.get(ERROR_MESSAGE_ATTRIBUTE).getAsString() : null;

                Result result = new Result(hookStatus, hookDuration, hookErrorMessage);

                JsonObject match = hook.get(MATCH_ATTRIBUTE).getAsJsonObject();

                String location = match.get(LOCATION_ATTRIBUTE).getAsString();
                if (!location.endsWith(")")) {
                    location = location.substring(0, location.lastIndexOf(")") + 1);
                }

                final List<Embedding> hookEmbeddings = new ArrayList<>();
                addEmbeddings(hook, hookEmbeddings);

                final List<String> hookOutputs = new ArrayList<>();
                addOutputs(hook, hookOutputs);

                hooks.add(new Hook(location, result, hookEmbeddings, hookOutputs));
            }
        }
    }

    private void addEmbeddings(JsonObject source, List<Embedding> embeddingList) {
        JsonArray embeddings = (JsonArray) source.get(EMBEDDINGS_ATTRIBUTE);

        if (embeddings != null) {
            for (JsonElement embedding : embeddings) {
                JsonObject embeddedData = embedding.getAsJsonObject();

                String data = embeddedData.get(DATA_ATTRIBUTE).getAsString();
                String mimeType = embeddedData.get(MIME_TYPE_ATTRIBUTE).getAsString();

                embeddingList.add(new Embedding(data, mimeType));
            }
        }
    }

    private void addOutputs(JsonObject source, List<String> outputList) {
        JsonArray output = (JsonArray) source.get(OUTPUT_ATTRIBUTE);

        if (output != null) {
            for (JsonElement out : output) {
                outputList.add(out.getAsString());
            }
        }
    }

    private void addStepRowData(JsonObject source, List<String> rowData) {
        JsonArray rows = (JsonArray) source.get(ROWS_ATTRIBUTE);

        if (rows != null) {
            rows.iterator().forEachRemaining(c -> {
                JsonArray cellArray = c.getAsJsonObject().get(CELLS_ATTRIBUTE).getAsJsonArray();

                StringBuilder cell = new StringBuilder();

                cellArray.iterator().forEachRemaining(t -> cell.append(t.getAsString()).append(" | "));

                if (cell.length() > 0) {
                    rowData.add("| " + cell.toString());
                }
            });
        }
    }
}