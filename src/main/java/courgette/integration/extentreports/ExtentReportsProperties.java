package courgette.integration.extentreports;

import courgette.runtime.CourgetteException;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.utils.FileUtils;

import java.io.File;

public class ExtentReportsProperties {
    private final String reportPath;
    private final String environmentInfo;

    public ExtentReportsProperties(CourgetteProperties courgetteProperties) {
        this.reportPath = courgetteProperties.getCourgetteOptions().reportTargetDir() + "/courgette-extentreports/";
        this.environmentInfo = courgetteProperties.getCourgetteOptions().environmentInfo();
        createReportDir();
    }

    public String getReportFilename() {
        return reportPath + "index.html";
    }

    public File getXMLConfigFile() {
        return FileUtils.getClassPathFile("extent-config.xml");
    }

    public String getEnvironmentInfo() {
        return environmentInfo;
    }

    private void createReportDir() {
        final File reportDir = new File(reportPath);

        if (!reportDir.exists()) {
            if (!reportDir.mkdirs()) {
                throw new CourgetteException("Unable to create the '../courgette-extentreports/' directory");
            }
        }
    }
}
