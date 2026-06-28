package courgette.runtime;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

class CucumberReportPublisher {

    private final String CUCUMBER_PUBLISH_URL = System.getProperty(
            "cucumber.publish.url",
            "https://messages.cucumber.io/api/reports");

    private final String CUCUMBER_PUBLISH_TOKEN = "CUCUMBER_PUBLISH_TOKEN";

    private final Pattern CUCUMBER_REPORT_URL = Pattern.compile("https://reports\\.cucumber\\.io/reports/\\S+");
    private final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[;\\d]*m");

    private File messageFile;

    private HttpClient httpClient;
    private List<String> errors = new ArrayList<>();

    public CucumberReportPublisher(File messageFile) {
        if (messageFile.exists()) {
            this.messageFile = messageFile;
            this.httpClient = createHttpClient();
        } else {
            errors.add("There are no Cucumber messages to publish.");
        }
    }

    public Optional<String> publish() {
        String reportUrl = null;

        final String token = System.getProperty(CUCUMBER_PUBLISH_TOKEN, System.getenv(CUCUMBER_PUBLISH_TOKEN));

        if (httpClient != null && errors.isEmpty()) {
            final Report report = createReport(token);
            if (report != null) {
                reportUrl = publishReport(report);
            }
        }

        if (reportUrl == null && !errors.isEmpty()) {
            System.err.println("Courgette was unable to publish the Cucumber Report. Reason(s): " + createErrorString());
        }
        return Optional.ofNullable(reportUrl);
    }

    private Report createReport(String token) {
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
                        String reportUrl = extractReportUrl(readResponse(response));
                        if (reportUrl.isEmpty()) {
                            errors.add("No Cucumber Report URL received from server.");
                            return null;
                        }
                        return new Report(location.getValue(), reportUrl);
                    }
                    errors.add("No upload location received from server.");
                } else {
                    errors.add(readResponse(response));
                }
            } else {
                errors.add("No response received from server.");
            }
        } catch (IOException e) {
            errors.add(e.getMessage());
        }
        return null;
    }

    private String publishReport(Report report) {
        try {
            HttpEntity entity = new ByteArrayEntity(gzip(messageFile));
            HttpPut reportMessage = new HttpPut(report.uploadUrl);
            reportMessage.addHeader("Content-Encoding", "gzip");
            reportMessage.setEntity(entity);
            HttpResponse response = httpClient.execute(reportMessage);
            if (response != null) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return report.displayUrl;
                } else {
                    errors.add(readResponse(response));
                }
            } else {
                errors.add("No response received from server.");
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
        return null;
    }

    private String readResponse(HttpResponse response) throws IOException {
        return response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), "UTF-8");
    }

    private String extractReportUrl(String response) {
        Matcher matcher = CUCUMBER_REPORT_URL.matcher(ANSI_ESCAPE.matcher(response).replaceAll(""));
        return matcher.find() ? matcher.group() : "";
    }

    private static class Report {
        private final String uploadUrl;
        private final String displayUrl;

        private Report(String uploadUrl, String displayUrl) {
            this.uploadUrl = uploadUrl;
            this.displayUrl = displayUrl;
        }
    }

    private byte[] gzip(File file) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            Files.copy(file.toPath(), gzipOutputStream);
        }
        return byteArrayOutputStream.toByteArray();
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
