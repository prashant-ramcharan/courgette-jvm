package courgette.runtime;

import courgette.runtime.utils.FileUtils;
import io.cucumber.messages.types.Envelope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.JacksonUtils.CUCUMBER_OBJECT_MAPPER;

final class CucumberNdJsonReporter {

    static void createReport(String fileName, List<Envelope> messages) {
        try {
            final FileWriter fileWriter = new FileWriter(fileName, false);

            for (Envelope message : messages) {
                fileWriter.write(CUCUMBER_OBJECT_MAPPER.writeValueAsString(message));
                fileWriter.write("\n");
            }

            fileWriter.close();

        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
    }

    static void copyReport(String ndJsonReportFile, String destinationNdJsonFile) {
        try {
            InputStream sourceNdJsonFile = new FileInputStream(ndJsonReportFile);

            FileUtils.copyInputStreamToFile(sourceNdJsonFile, new File(destinationNdJsonFile));

        } catch (Exception e) {
            printExceptionStackTrace(e);
        }
    }
}
