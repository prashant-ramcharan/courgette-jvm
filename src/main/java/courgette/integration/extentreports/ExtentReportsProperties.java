package courgette.integration.extentreports;

import courgette.runtime.CourgetteException;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.utils.FileUtils;

import java.io.File;

public class ExtentReportsProperties {
    private final CourgetteProperties courgetteProperties;

    public ExtentReportsProperties(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        createImagesDir();
    }

    public String getReportPath() {
        return courgetteProperties.getCourgetteOptions().reportTargetDir() + "/courgette-extentreports/index.html";
    }

    public String getReportImagesPath() {
        return courgetteProperties.getCourgetteOptions().reportTargetDir() + "/courgette-extentreports/images/";
    }

    public File getXMLConfigFile() {
        return FileUtils.getClassPathFile("extent-config.xml");
    }

    private void createImagesDir() {
        final File imagesDir = new File(getReportImagesPath());

        if (!imagesDir.exists()) {
            if (!imagesDir.mkdirs()) {
                throw new CourgetteException("Unable to create the '../courgette-extentreports/images' directory");
            }
        }
    }
}
