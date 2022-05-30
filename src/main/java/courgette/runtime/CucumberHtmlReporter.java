package courgette.runtime;

import io.cucumber.htmlformatter.MessagesToHtmlWriter;
import io.cucumber.messages.types.Envelope;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.JacksonUtils.CUCUMBER_OBJECT_MAPPER;

public final class CucumberHtmlReporter {

    static void createReport(String fileName, List<Envelope> messages) {
        try {
            final FileOutputStream outputStream = new FileOutputStream(fileName, false);

            final MessagesToHtmlWriter htmlWriter = new MessagesToHtmlWriter(outputStream, CUCUMBER_OBJECT_MAPPER::writeValue);

            for (Envelope message : messages) {
                htmlWriter.write(message);
            }

            htmlWriter.close();

        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
    }
}
