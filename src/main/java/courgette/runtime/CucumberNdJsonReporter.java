package courgette.runtime;

import io.cucumber.messages.JSON;
import io.cucumber.messages.types.Envelope;
import org.testng.reporters.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

final class CucumberNdJsonReporter {

    static void createReport(String fileName, List<Envelope> messages) {
        try {
            final FileWriter fileWriter = new FileWriter(fileName, false);

            for (Envelope message : messages) {
                fileWriter.write(JSON.writeValueAsString(message));
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

            Files.copyFile(sourceNdJsonFile, new File(destinationNdJsonFile));

        } catch (Exception e) {
            printExceptionStackTrace(e);
        }
    }
}
