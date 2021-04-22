package courgette.integration.reportportal.request;

import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;

public class FinishRequest {

    public HttpEntity create(final String launchId) {
        final JsonObject body = new JsonObject();

        body.addProperty("endTime", Instant.now().toString());
        body.addProperty("launchUuid", launchId);

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }

    public HttpEntity create(final String status, final String launchId) {
        final JsonObject body = new JsonObject();

        body.addProperty("endTime", Instant.now().toString());
        body.addProperty("status", status);
        body.addProperty("launchUuid", launchId);

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }
}
