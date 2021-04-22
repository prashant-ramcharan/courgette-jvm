package courgette.integration.reportportal.request;

import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;

public class TestRequest {

    public static HttpEntity create(String featureName, final String launchId) {
        final JsonObject body = new JsonObject();

        body.addProperty("name", featureName);
        body.addProperty("startTime", Instant.now().toString());
        body.addProperty("type", "test");
        body.addProperty("launchUuid", launchId);

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }
}
