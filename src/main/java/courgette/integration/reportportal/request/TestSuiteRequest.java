package courgette.integration.reportportal.request;

import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;

public class TestSuiteRequest {

    public HttpEntity create(final String name, final String launchId) {
        final JsonObject body = new JsonObject();

        body.addProperty("name", name);
        body.addProperty("startTime", Instant.now().toString());
        body.addProperty("type", "suite");
        body.addProperty("launchUuid", launchId);

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }
}
