package courgette.integration.reportportal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import courgette.runtime.utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static java.util.Arrays.asList;

public class ReportPortalService {
    private final ReportPortalProperties reportPortalProperties;
    private final HttpClient httpClient;
    private final String authorization;
    private final String importUrl;
    private final String latestLaunchUrl;

    private final String API_LAUNCH_IMPORT = "api/v1/%s/launch/import";
    private final String API_LAUNCH_LATEST = "api/v1/%s/launch/latest";
    private final String API_LAUNCH_UPDATE = "api/v1/%s/launch/%d/update";

    private Integer launchId;

    private ReportPortalService(ReportPortalProperties reportPortalProperties) {
        this.reportPortalProperties = reportPortalProperties;
        this.httpClient = createHttpClient();
        this.authorization = "bearer " + reportPortalProperties.getApiToken();
        this.importUrl = reportPortalProperties.getEndpoint() + String.format(API_LAUNCH_IMPORT, reportPortalProperties.getProject());
        this.latestLaunchUrl = reportPortalProperties.getEndpoint() + String.format(API_LAUNCH_LATEST, reportPortalProperties.getProject());
    }

    public static ReportPortalService create(ReportPortalProperties reportPortalProperties) {
        return new ReportPortalService(reportPortalProperties);
    }

    public boolean publishReport(String reportFilename) {
        File zipFile = FileUtils.zipFile(reportFilename, true);

        boolean published = false;

        if (zipFile.exists()) {
            final Optional<HttpResponse> response = sendMultiPartPost(zipFile);
            published = handleOKResponse(response);

            if (published) {
                setLatestLaunchId();
            }
        }
        return published;
    }

    public void updateLaunchTags() {
        final String tags = reportPortalProperties.getTags();

        if (launchId != null && !tags.isEmpty()) {
            String launchUpdateUrl = reportPortalProperties.getEndpoint() + String.format(API_LAUNCH_UPDATE, reportPortalProperties.getProject(), launchId);

            String updateRequest = createLaunchUpdateRequest(tags);
            final Optional<HttpResponse> response = updateLaunchTags(launchUpdateUrl, updateRequest);
            handleOKResponse(response);
        }
    }

    private Optional<HttpResponse> sendMultiPartPost(File file) {
        try {
            HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .addBinaryBody("file", file)
                    .build();

            HttpPost importReport = new HttpPost(importUrl);
            importReport.addHeader("Authorization", authorization);
            importReport.setEntity(entity);
            return Optional.of(httpClient.execute(importReport));
        } catch (Exception e) {
            printError("publish the report", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<HttpResponse> updateLaunchTags(String launchUpdateUrl, String updateRequest) {
        HttpPut launchUpdate = new HttpPut(launchUpdateUrl);
        launchUpdate.addHeader("Authorization", authorization);

        HttpEntity entity = EntityBuilder
                .create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(updateRequest)
                .build();

        launchUpdate.setEntity(entity);

        try {
            return Optional.ofNullable(httpClient.execute(launchUpdate));
        } catch (Exception e) {
            printError("update launch tags", e.getMessage());
        }
        return Optional.empty();
    }

    private void setLatestLaunchId() {
        HttpGet latestLaunch = new HttpGet(latestLaunchUrl);
        latestLaunch.addHeader("Authorization", authorization);

        try {
            HttpResponse response = httpClient.execute(latestLaunch);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String body = EntityUtils.toString(response.getEntity(), "UTF-8");
                Optional<Integer> id = extractLaunchId(body);
                id.ifPresent(i -> launchId = i);
            }
        } catch (Exception e) {
            printError("get the latest launch id", e.getMessage());
        }
    }

    private Optional<Integer> extractLaunchId(String body) {
        JsonElement json = JsonParser.parseString(body);
        if (json instanceof JsonNull) {
            return Optional.empty();
        }

        JsonElement content = json.getAsJsonObject().get("content");
        if (content instanceof JsonNull) {
            return Optional.empty();
        }

        JsonArray contentDetails = content.getAsJsonArray();
        JsonElement id = contentDetails.get(contentDetails.size() - 1).getAsJsonObject().get("id");
        return (id instanceof JsonNull) ? Optional.empty() : Optional.of(id.getAsInt());
    }

    private String createLaunchUpdateRequest(String tags) {
        JsonObject jsonObject = new JsonObject();
        JsonArray attributeArr = new JsonArray();

        String[] tagData = tags.split(";");

        asList(tagData).forEach(tag -> {
            JsonObject tagObj = new JsonObject();
            tagObj.add("key", JsonNull.INSTANCE);
            tagObj.add("value", new JsonPrimitive(tag));
            attributeArr.add(tagObj);
        });

        jsonObject.add("attributes", attributeArr);
        jsonObject.add("mode", new JsonPrimitive("DEFAULT"));
        return jsonObject.toString();
    }

    private boolean handleOKResponse(Optional<HttpResponse> response) {
        if (!response.isPresent()) {
            return false;
        }

        if (response.get().getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            String body;
            try {
                body = EntityUtils.toString(response.get().getEntity(), "UTF-8");
            } catch (IOException e) {
                body = e.getMessage();
            }
            printError(body);
            return false;
        }
        return true;
    }

    private HttpClient createHttpClient() {
        try {
            SSLContext trustedSSLContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
            return HttpClientBuilder.create().setSSLContext(trustedSSLContext).build();
        } catch (Exception e) {
            printError("error creating a secure http client: " + e.getMessage());
        }
        return HttpClientBuilder.create().build();
    }

    private void printError(String error) {
        System.err.format("Courgette Report Portal Service: %s\n", error);
    }

    private void printError(String action, String error) {
        System.err.format("Courgette Report Portal Service: unable to %s on the report portal server, reason: %s\n", action, error);
    }
}
