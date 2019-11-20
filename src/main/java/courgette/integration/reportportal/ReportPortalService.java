package courgette.integration.reportportal;

import courgette.runtime.utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;

public class ReportPortalService {
    private ReportPortalProperties reportPortalProperties;

    private final String API_RESOURCE = "api/v1/%s/launch/import";

    private ReportPortalService(ReportPortalProperties reportPortalProperties) {
        this.reportPortalProperties = reportPortalProperties;
    }

    public static ReportPortalService create(ReportPortalProperties reportPortalProperties) {
        return new ReportPortalService(reportPortalProperties);
    }

    public void publishReport(String reportFilename) {
        String projectEndpoint = reportPortalProperties.getEndpoint() + String.format(API_RESOURCE, reportPortalProperties.getProject());
        String authorization = "bearer " + reportPortalProperties.getApiToken();

        File zipFile = FileUtils.zipFile(reportFilename, true);
        if (zipFile.exists()) {
            final HttpResponse response = sendMultiPartPost(projectEndpoint, authorization, zipFile);

            if (response != null && response.getStatusLine().getStatusCode() != 200) {
                String body;
                try {
                    body = EntityUtils.toString(response.getEntity(), "UTF-8");
                } catch (IOException e) {
                    body = e.getMessage();
                }
                System.err.format("Unable to send the report to report portal server, reason: %s", body);
            }
        }
    }

    private HttpResponse sendMultiPartPost(String url, String authorization, File file) {
        try {
            SSLContext trustedSSLContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();

            HttpClient httpClient = HttpClientBuilder.create().setSSLContext(trustedSSLContext).build();

            HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .addBinaryBody("file", file)
                    .build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Authorization", authorization);
            httpPost.setEntity(entity);
            return httpClient.execute(httpPost);
        } catch (Exception e) {
            System.err.format("Unable to send the report to report portal server, reason: %s", e.getMessage());
            return null;
        }
    }
}
