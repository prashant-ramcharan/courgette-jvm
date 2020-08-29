package courgette.runtime;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class CucumberReportPublisher {

    private final String CUCUMBER_PUBLISH_URL = System.getProperty(
            "cucumber.publish.url",
            "https://messages.cucumber.io/api/reports");

    private final String CUCUMBER_REPORT_URL = "https://reports.cucumber.io";

    private List<String> messages;

    private HttpClient httpClient;
    private List<String> errors = new ArrayList<>();

    public CucumberReportPublisher(List<String> messages) {
        if (!messages.isEmpty()) {
            this.messages = messages;
            this.httpClient = createHttpClient();
        } else {
            errors.add("There are no Cucumber messages to publish");
        }
    }

    public Optional<String> publish() {
        String reportUrl = null;

        if (httpClient != null && errors.isEmpty()) {
            final String report = createReport();
            if (report != null) {
                reportUrl = publishReport(report);
            }
        }

        if (reportUrl == null && !errors.isEmpty()) {
            System.err.println("Unable to publish the Cucumber Report. Reason(s): " + Arrays.toString(errors.toArray()));
        }
        return Optional.ofNullable(reportUrl);
    }

    private String createReport() {
        try {
            HttpGet resource = new HttpGet(CUCUMBER_PUBLISH_URL);
            HttpResponse response = httpClient.execute(resource);
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                    Header location = response.getFirstHeader("Location");
                    if (location != null && !location.getValue().isEmpty()) {
                        return location.getValue();
                    }
                } else {
                    errors.add(String.format("Create Cucumber Report: expected status code %d but received status code %d", HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode()));
                }
            } else {
                errors.add("Create Cucumber Report: no response received from server");
            }
        } catch (IOException e) {
            errors.add("Create Cucumber Report: " + e.getMessage());
        }
        return null;
    }

    private String publishReport(String resourceUrl) {
        HttpEntity entity = EntityBuilder.create().setText(String.join("\n", messages)).build();

        try {
            HttpPut reportMessage = new HttpPut(resourceUrl);
            reportMessage.setEntity(entity);
            HttpResponse response = httpClient.execute(reportMessage);
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return CUCUMBER_REPORT_URL + URI.create(resourceUrl).getPath();
                } else {
                    errors.add(String.format("Publish Cucumber Report: expected status code %d but received status code %d", HttpStatus.SC_OK, response.getStatusLine().getStatusCode()));
                }
            } else {
                errors.add("Publish Cucumber Report: no response received from server");
            }
        } catch (Exception e) {
            errors.add("Publish Cucumber Report: " + e.getMessage());
        }
        return null;
    }

    private HttpClient createHttpClient() {
        try {
            SSLContext trustedSSLContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
            return HttpClientBuilder.create().setSSLContext(trustedSSLContext).build();
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
        return null;
    }
}
