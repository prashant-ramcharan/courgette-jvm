package courgette.runtime;

import io.cucumber.htmlformatter.MessagesToHtmlWriter;
import io.cucumber.messages.Messages;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

public final class CucumberHtmlReporter {

    static void createReport(String fileName, List<Messages.Envelope> messages) {

        try {
            final FileWriter reportWriter = new FileWriter(fileName, false);

            final MessagesToHtmlWriter htmlWriter = new MessagesToHtmlWriter(reportWriter);

            for (Messages.Envelope message : messages) {
                htmlWriter.write(message);
            }

            htmlWriter.close();

        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
    }
}
