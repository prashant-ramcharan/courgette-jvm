package courgette.integration.reportportal.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.util.List;

public class ScenarioRequest {

    public static HttpEntity create(final String scenarioName, List<String> scenarioTags, final String launchId) {
        final JsonObject body = new JsonObject();

        body.addProperty("name", scenarioName);
        body.addProperty("launchUuid", launchId);
        body.addProperty("startTime", Instant.now().toString());
        body.addProperty("type", "step");

        addAttributes(body, scenarioTags);

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }

    private static void addAttributes(JsonObject json, List<String> tags) {
        if (!tags.isEmpty()) {

            JsonArray attributeArr = new JsonArray();

            tags.forEach(tag -> {
                JsonObject tagObj = new JsonObject();
                tagObj.add("key", JsonNull.INSTANCE);
                tagObj.add("value", new JsonPrimitive(tag));
                attributeArr.add(tagObj);
            });

            json.add("attributes", attributeArr);
        }
    }
}
