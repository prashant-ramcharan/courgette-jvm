package courgette.integration.reportportal;

import courgette.runtime.CourgetteException;
import courgette.runtime.utils.FileUtils;
import io.restassured.response.Response;

import java.io.File;

import static io.restassured.RestAssured.given;

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

        try {
            File zipFile = FileUtils.zipFile(reportFilename);
            if (zipFile.exists()) {
                final Response response = given()
                        .header("Authorization", "bearer " + reportPortalProperties.getApiKey())
                        .multiPart(zipFile)
                        .post(projectEndpoint);

                if (response.getStatusCode() != 200) {
                    System.err.format("Unable to send the report to report portal server, reason: %s", response.getBody().print());
                }
            }
        } catch (CourgetteException ex) {
            System.err.format("Unable to send the report to report portal server, reason: %s", ex.getMessage());
        }
    }
}
