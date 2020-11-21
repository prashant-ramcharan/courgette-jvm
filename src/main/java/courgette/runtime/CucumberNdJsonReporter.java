package courgette.runtime;

import io.cucumber.messages.Messages;
import io.cucumber.messages.internal.com.google.protobuf.util.JsonFormat;
import org.testng.reporters.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

final class CucumberNdJsonReporter {

    static void createReport(String fileName, List<Messages.Envelope> messages) {
        try {
            final FileWriter fileWriter = new FileWriter(fileName, false);

            final JsonFormat.Printer jsonPrinter = JsonFormat.printer().omittingInsignificantWhitespace();

            for (Messages.Envelope message : messages) {
                fileWriter.write(jsonPrinter.print(message));
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
