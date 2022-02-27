package courgette.runtime;

import courgette.runtime.event.CourgetteEvent;

import java.util.List;

public class CourgetteSlackOptions {

    private final String webhookUrl;
    private final List<String> channels;
    private final String testId;
    private final List<CourgetteEvent> events;

    public CourgetteSlackOptions(String webhookUrl, List<String> channels, String testId, List<CourgetteEvent> events) {
        this.webhookUrl = webhookUrl;
        this.channels = channels;
        this.testId = testId;
        this.events = events;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public List<String> getChannels() {
        return channels;
    }

    public String getTestId() {
        return testId;
    }

    public List<CourgetteEvent> getEvents() {
        return events;
    }

    public boolean shouldValidate() {
        return webhookUrl.trim().length() > 0 || !channels.isEmpty();
    }

    public boolean isValid() {
        return webhookUrl.trim().length() > 0
                && !channels.isEmpty()
                && channels.stream().noneMatch(c -> c.trim().length() == 0);
    }
}
