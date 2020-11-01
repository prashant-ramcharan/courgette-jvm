package courgette.runtime;

import io.cucumber.htmlformatter.MessagesToHtmlWriter;
import io.cucumber.messages.Messages;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class CucumberHtmlReporter {

    public String createHtmlReport(List<Messages.Envelope> messages) {
        final StringWriter writer = new StringWriter();

        try {
            final MessagesToHtmlWriter htmlWriter = new MessagesToHtmlWriter(writer);

            for (Messages.Envelope message : messages) {
                htmlWriter.write(message);
            }

            htmlWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }
}
