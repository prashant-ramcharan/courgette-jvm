package courgette.integration.reportportal;

import courgette.runtime.CourgetteException;
import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ReportPortalProperties {
    private Properties reportPortalProperties = new Properties();

    public ReportPortalProperties() {
        try {
            File reportPortalPropertiesFile = FileUtils.getClassPathFile("reportportal.properties");
            this.reportPortalProperties.load(new FileInputStream(reportPortalPropertiesFile));
        } catch (IOException e) {
            throw new CourgetteException("Unable to load the reportportal properties from the classpath");
        }
    }

    public String getApiToken() {
        return reportPortalProperties.getProperty("rp.apitoken");
    }


    public String getEndpoint() {
        String endpoint = reportPortalProperties.getProperty("rp.endpoint");

        if (endpoint != null && !endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        return endpoint;
    }

    public String getProject() {
        return reportPortalProperties.getProperty("rp.project");
    }

    public void validate() {
        String apiToken = getApiToken();
        if (apiToken == null) {
            throw new CourgetteException("Report portal api token (rp.apitoken) is missing from the reportportal.properties");
        }

        String endpoint = getEndpoint();
        if (endpoint == null) {
            throw new CourgetteException("Report portal endpoint (rp.endpoint) is missing from the reportportal.properties");
        }

        String project = getProject();
        if (project == null) {
            throw new CourgetteException("Report portal project (rp.project) is missing from the reportportal.properties");
        }
    }
}
