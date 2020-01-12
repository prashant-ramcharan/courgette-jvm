package courgette.runtime.report;

import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.*;
import gherkin.deps.com.google.gson.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class JsonReportParser {
    private File jsonFile;
    private List<Feature> features;

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

    private JsonReportParser(File jsonFile) {
        this.jsonFile = jsonFile;
        this.features = new ArrayList<>();
    }

    public static JsonReportParser create(File jsonFile) {
        return new JsonReportParser(jsonFile);
    }

    public List<Feature> getReportFeatures() {
        try {
            parseJsonReport();
        } catch (FileNotFoundException | IllegalStateException e) {
            throw new CourgetteException(e);
        }
        return features;
    }

    private void parseJsonReport() throws FileNotFoundException {
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

            final Map<Integer, JsonArray> backgroundSteps = new HashMap<>();

            while (elementsIterator.hasNext()) {
                JsonObject element = elementsIterator.next().getAsJsonObject();

                int elementLine = element.get(LINE_ATTRIBUTE).getAsInt();

                if (element.get(KEYWORD_ATTRIBUTE).getAsString().equalsIgnoreCase("Background")) {
                    JsonArray elementSteps = (JsonArray) element.get(STEPS_ATTRIBUTE);
                    backgroundSteps.put(elementLine, elementSteps);
                }
            }

            elementsIterator = elements.iterator();

            final List<Scenario> scenarioElements = new ArrayList<>();

            while (elementsIterator.hasNext()) {
                JsonObject scenario = elementsIterator.next().getAsJsonObject();

                String scenarioName = scenario.get(NAME_ATTRIBUTE).getAsString();
                String scenarioKeyword = scenario.get(KEYWORD_ATTRIBUTE).getAsString();
                int scenarioLine = scenario.get(LINE_ATTRIBUTE).getAsInt();

                final List<Hook> scenarioBefore = new ArrayList<>();
                addHook(scenario.get(BEFORE_ATTRIBUTE), scenarioBefore);

                final List<Hook> scenarioAfter = new ArrayList<>();
                addHook(scenario.get(AFTER_ATTRIBUTE), scenarioAfter);

                List<JsonArray> allSteps = new ArrayList<>(backgroundSteps.values());
                allSteps.addAll(Collections.singleton((JsonArray) scenario.get(STEPS_ATTRIBUTE)));

                final List<Step> scenarioSteps = new ArrayList<>();
                allSteps.forEach(steps -> addSteps(steps, scenarioSteps));

                final List<Tag> scenarioTags = new ArrayList<>();
                addTags(scenario.get(TAGS_ATTRIBUTE), scenarioTags);

                scenarioElements.add(new Scenario(featureUri, scenarioName, scenarioKeyword, scenarioLine, scenarioBefore, scenarioAfter, scenarioSteps, scenarioTags));
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