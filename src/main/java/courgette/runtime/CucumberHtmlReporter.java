package courgette.runtime;

import io.cucumber.htmlformatter.MessagesToHtmlWriter;
import io.cucumber.messages.types.Envelope;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.JacksonUtils.CUCUMBER_OBJECT_MAPPER;

public final class CucumberHtmlReporter {
    private MessagesToHtmlWriter htmlWriter;
    private int reportSize;
    private final Map<String, String> errors = new HashMap<>();

    public CucumberHtmlReporter(String fileName, int reportSize) {
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName, false);
            htmlWriter = new MessagesToHtmlWriter(outputStream, CUCUMBER_OBJECT_MAPPER::writeValue);
            this.reportSize = reportSize;
        } catch (IOException e) {
            errors.put(UUID.randomUUID().toString(), e.getMessage());
            printExceptionStackTrace(e);
        }
    }

    public void writeReport(List<Envelope> messages) {
        try {
            for (Envelope message : messages) {
                htmlWriter.write(message);
            }

            reportSize--;

            if (reportSize == 0) {
                htmlWriter.close();
            }
        } catch (IOException e) {
            errors.put(UUID.randomUUID().toString(), e.getMessage());
            printExceptionStackTrace(e);
        }
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
