package courgette.integration.reportportal.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import courgette.integration.reportportal.ReportPortalProperties;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.util.Collections;

public class LaunchRequest {

    public HttpEntity create(final ReportPortalProperties reportPortalProperties) {
        final JsonObject body = new JsonObject();

        body.addProperty("name", reportPortalProperties.getLaunchName());
        body.addProperty("startTime", Instant.now().toString());
        body.addProperty("mode", "DEFAULT");

        addAttributes(body, reportPortalProperties.getAttributes());

        return new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
    }


    private static void addAttributes(JsonObject body, final String attributes) {

        if (attributes != null && !attributes.trim().isEmpty()) {

            final String[] attributeKeyValue = attributes.split(";");

            if (attributeKeyValue.length > 0) {
                final JsonArray attributeArray = new JsonArray();

                for (String attributePair : attributeKeyValue) {
                    final JsonObject attributeObj = new JsonObject();

                    Collections.singletonList(attributePair).forEach(attr -> {
                        final String[] attrKeyValue = attr.split(":");

                        if (attrKeyValue.length > 0) {
                            switch (attrKeyValue.length) {
                                case 1:
                                    attributeObj.addProperty("value", attrKeyValue[0]);
                                    break;
                                case 2:
                                    attributeObj.addProperty("key", attrKeyValue[0]);
                                    attributeObj.addProperty("value", attrKeyValue[1]);
                                    break;
                            }

                            attributeArray.add(attributeObj);
                        }
                    });
                }

                body.add("attributes", attributeArray);
            }
        }
    }
}