package courgette.integration.reportportal;

import courgette.runtime.CourgetteException;
import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ReportPortalProperties {
    private Properties reportPortalProperties = new Properties();

    private ReportPortalProperties() {
        try {
            File reportPortalPropertiesFile = FileUtils.getClassPathFile("reportportal.properties");
            this.reportPortalProperties.load(new FileInputStream(reportPortalPropertiesFile));
        } catch (IOException e) {
            throw new CourgetteException("Unable to load the reportportal properties from the classpath");
        }
    }

    private static ReportPortalProperties instance;

    public static ReportPortalProperties getInstance() {
        if (instance == null) {
            instance = new ReportPortalProperties();
        }
        return instance;
    }

    public String getApiToken() {
        return getProperty("rp.apitoken");
    }

    public String getEndpoint() {
        String endpoint = getProperty("rp.endpoint");

        if (endpoint != null && !endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        return endpoint;
    }

    public String getProject() {
        return getProperty("rp.project");
    }

    public String getLaunchName() {
        return getProperty("rp.launch", "Courgette Test Execution").trim();
    }

    public String getTestSuite() {
        return getProperty("rp.testsuite", "Test Suite").trim();
    }

    public String getTags() {
        return getProperty("rp.tags", "").trim();
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

    private String getProperty(String property) {
        return System.getProperty(property, reportPortalProperties.getProperty(property));
    }

    private String getProperty(String property, String def) {
        Object defaultValue = reportPortalProperties.getOrDefault(property, def);
        return System.getProperty(property, String.valueOf(defaultValue));
    }
}
