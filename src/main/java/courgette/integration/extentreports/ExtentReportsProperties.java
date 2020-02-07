package courgette.integration.extentreports;

import courgette.runtime.CourgetteException;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.utils.FileUtils;

import java.io.File;

public class ExtentReportsProperties {
    private final String reportPath;

    public ExtentReportsProperties(CourgetteProperties courgetteProperties) {
        this.reportPath = courgetteProperties.getCourgetteOptions().reportTargetDir() + "/courgette-extentreports/";
        createReportDir();
    }

    public String getReportFilename() {
        return reportPath + "index.html";
    }

    public File getXMLConfigFile() {
        return FileUtils.getClassPathFile("extent-config.xml");
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
