package courgette.runtime.report;

import courgette.runtime.CourgetteException;
import courgette.runtime.report.model.*;
import gherkin.deps.com.google.gson.JsonArray;
import gherkin.deps.com.google.gson.JsonElement;
import gherkin.deps.com.google.gson.JsonObject;
import gherkin.deps.com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class JsonReportParser {
    private File jsonFile;
    private List<Feature> features;

    private final static String NAME_ATTRIBUTE = "name";
    private final static String URI_ATTRIBUTE = "uri";
    private final static String ELEMENTS_ATTRIBUTE = "elements";
    private final static String STEPS_ATTRIBUTE = "steps";
    private final static String KEYWORD_ATTRIBUTE = "keyword";
    private final static String RESULT_ATTRIBUTE = "result";
    private final static String STATUS_ATTRIBUTE = "status";
    private final static String DURATION_ATTRIBUTE = "duration";
    private final static String ERROR_MESSAGE_ATTRIBUTE = "error_message";
    private final static String BEFORE_ATTRIBUTE = "before";
    private final static String AFTER_ATTRIBUTE = "after";
    private final static String MATCH_ATTRIBUTE = "match";
    private final static String LOCATION_ATTRIBUTE = "location";
    private final static String EMBEDDINGS_ATTRIBUTE = "embeddings";
    private final static String OUTPUT = "output";
    private final static String DATA_ATTRIBUTE = "data";
    private final static String MIME_TYPE_ATTRIBUTE = "mime_type";

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
        } catch (FileNotFoundException e) {
            throw new CourgetteException(e);
        }
        return features;
    }

    private void parseJsonReport() throws FileNotFoundException {
        JsonParser jsonParser = new JsonParser();
        JsonArray reportJson = (JsonArray) jsonParser.parse(new FileReader(jsonFile));

        for (JsonElement report : reportJson) {
            final JsonObject feature = report.getAsJsonObject();

            String featureName = feature.get(NAME_ATTRIBUTE).getAsString();
            String featureUri = feature.get(URI_ATTRIBUTE).getAsString();

            JsonArray elements = (JsonArray) feature.get(ELEMENTS_ATTRIBUTE);
            Iterator<JsonElement> elementsIterator = elements.iterator();

            final List<Scenario> scenarioElements = new ArrayList<>();

            while (elementsIterator.hasNext()) {
                JsonObject scenario = elementsIterator.next().getAsJsonObject();

                Function<String, List<Hook>> hookFunc = (attr) -> {
                    final List<Hook> hookList = new ArrayList<>();

                    JsonArray scenarioHooks = (JsonArray) scenario.get(attr);
                    if (scenarioHooks != null) {
                        for (JsonElement scenarioHook : scenarioHooks) {
                            JsonObject hook = scenarioHook.getAsJsonObject();

                            JsonObject hookResult = hook.get(RESULT_ATTRIBUTE).getAsJsonObject();
                            String hookStatus = hookResult.get(STATUS_ATTRIBUTE).getAsString();
                            long hookDuration = hookResult.get(DURATION_ATTRIBUTE) != null ? hookResult.get(DURATION_ATTRIBUTE).getAsLong() : 0L;
                            String hookErrorMessage = hookResult.get(ERROR_MESSAGE_ATTRIBUTE) != null ? hookResult.get(ERROR_MESSAGE_ATTRIBUTE).getAsString() : null;

                            Result result = new Result(hookStatus, hookDuration, hookErrorMessage);

                            JsonObject match = hook.get(MATCH_ATTRIBUTE).getAsJsonObject();
                            String location = match.get(LOCATION_ATTRIBUTE).getAsString();

                            hookList.add(new Hook(location, result));
                        }
                    }
                    return hookList;
                };

                String scenarioName = scenario.get(NAME_ATTRIBUTE).getAsString();
                List<Hook> scenarioBefore = hookFunc.apply(BEFORE_ATTRIBUTE);
                List<Hook> scenarioAfter = hookFunc.apply(AFTER_ATTRIBUTE);

                JsonArray steps = (JsonArray) scenario.get(STEPS_ATTRIBUTE);
                final List<Step> scenarioSteps = new ArrayList<>();

                if (steps != null) {
                    steps.iterator().forEachRemaining((e) -> {
                        JsonObject step = e.getAsJsonObject();
                        JsonObject result = step.get(RESULT_ATTRIBUTE).getAsJsonObject();

                        String stepName = step.get(NAME_ATTRIBUTE).getAsString();
                        String stepKeyword = step.get(KEYWORD_ATTRIBUTE).getAsString();
                        String stepStatus = result.get(STATUS_ATTRIBUTE).getAsString();
                        long stepDuration = result.get(DURATION_ATTRIBUTE) != null ? result.get(DURATION_ATTRIBUTE).getAsLong() : 0L;
                        String stepErrorMessage = result.get(ERROR_MESSAGE_ATTRIBUTE) != null ? result.get(ERROR_MESSAGE_ATTRIBUTE).getAsString() : null;

                        Result stepResult = new Result(stepStatus, stepDuration, stepErrorMessage);

                        JsonArray embeddings = (JsonArray) step.get(EMBEDDINGS_ATTRIBUTE);
                        final List<Embedding> stepEmbeddings = new ArrayList<>();

                        if (embeddings != null) {
                            for (JsonElement embedding : embeddings) {
                                JsonObject embeddedData = embedding.getAsJsonObject();

                                String data = embeddedData.get(DATA_ATTRIBUTE).getAsString();
                                String mimeType = embeddedData.get(MIME_TYPE_ATTRIBUTE).getAsString();

                                stepEmbeddings.add(new Embedding(data, mimeType));
                            }
                        }

                        JsonArray output = (JsonArray) step.get(OUTPUT);
                        final List<String> stepOutputs = new ArrayList<>();

                        if (output != null) {
                            for (JsonElement out : output) {
                                stepOutputs.add(out.getAsString());
                            }
                        }
                        scenarioSteps.add(new Step(stepName, stepKeyword, stepResult, stepEmbeddings, stepOutputs));
                    });
                }
                scenarioElements.add(new Scenario(scenarioName, scenarioBefore, scenarioAfter, scenarioSteps));
            }
            features.add(new Feature(featureName, featureUri, scenarioElements));
        }
    }
}
