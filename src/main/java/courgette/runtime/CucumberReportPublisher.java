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
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class CucumberReportPublisher {

    private final String CUCUMBER_PUBLISH_URL = System.getProperty(
            "cucumber.publish.url",
            "https://messages.cucumber.io/api/reports");

    private final String CUCUMBER_REPORT_URL = "https://reports.cucumber.io/reports";

    private final String CUCUMBER_PUBLISH_TOKEN = "CUCUMBER_PUBLISH_TOKEN";

    private String report;

    private HttpClient httpClient;
    private List<String> errors = new ArrayList<>();

    public CucumberReportPublisher(String report) {
        if (!report.isEmpty()) {
            this.report = report;
            this.httpClient = createHttpClient();
        } else {
            errors.add("There are no Cucumber messages to publish.");
        }
    }

    public Optional<String> publish() {
        String reportUrl = null;

        final String token = System.getProperty(CUCUMBER_PUBLISH_TOKEN, System.getenv(CUCUMBER_PUBLISH_TOKEN));

        if (httpClient != null && errors.isEmpty()) {
            final String report = createReport(token);
            if (report != null) {
                reportUrl = publishReport(report);
            }
        }

        if (reportUrl == null && !errors.isEmpty()) {
            System.err.println("Courgette was unable to publish the Cucumber Report. Reason(s): " + createErrorString());
        }
        return Optional.ofNullable(reportUrl);
    }

    private String createReport(String token) {
        try {
            HttpGet resource = new HttpGet(CUCUMBER_PUBLISH_URL);

            if (token != null && !token.isEmpty()) {
                resource.addHeader("Authorization", "Bearer " + token);
            }

            HttpResponse response = httpClient.execute(resource);
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                    Header location = response.getFirstHeader("Location");
                    if (location != null && !location.getValue().isEmpty()) {
                        return location.getValue();
                    }
                } else {
                    errors.add(EntityUtils.toString(response.getEntity(), "UTF-8"));
                }
            } else {
                errors.add("No response received from server.");
            }
        } catch (IOException e) {
            errors.add(e.getMessage());
        }
        return null;
    }

    private String publishReport(String resourceUrl) {
        HttpEntity entity = EntityBuilder.create().setText(String.join("\n", report)).build();

        try {
            HttpPut reportMessage = new HttpPut(resourceUrl);
            reportMessage.setEntity(entity);
            HttpResponse response = httpClient.execute(reportMessage);
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    String reportPath = URI.create(resourceUrl).getPath();
                    reportPath = reportPath.substring(reportPath.lastIndexOf("/"));
                    return CUCUMBER_REPORT_URL + reportPath;
                } else {
                    errors.add(EntityUtils.toString(response.getEntity(), "UTF-8"));
                }
            } else {
                errors.add("No response received from server.");
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
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

    private String createErrorString() {
        StringBuilder errorBuilder = new StringBuilder();
        errors.forEach(e -> errorBuilder.append("\n").append(e));
        return errorBuilder.toString();
    }
}
