package courgette.integration.slack;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteSlackOptions;
import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.event.EventHolder;
import courgette.runtime.event.EventSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

public class SlackMessageSender implements EventSender {

    private final SlackService slackService;
    private final CourgetteSlackOptions slackOptions;
    private final Mustache messageTemplate;

    public SlackMessageSender(SlackService slackService, CourgetteSlackOptions slackOptions) {
        this.slackService = slackService;
        this.slackOptions = slackOptions;
        this.messageTemplate = readTemplate("/slack/message.mustache");
    }

    @Override
    public void send(EventHolder eventHolder) {
        slackOptions.getChannels().forEach(channel -> slackService.postMessage(createMessage(channel, eventHolder)));
    }

    private String createMessage(String channel, EventHolder eventHolder) {
        if (isInfoEvent(eventHolder)) {
            return createFromTemplate(messageTemplate, createInfoMessageData(channel, eventHolder));
        } else {
            return createFromTemplate(messageTemplate, createMessageData(channel, eventHolder));
        }
    }

    private boolean isInfoEvent(EventHolder eventHolder) {
        return eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RUN_STARTED)
                || eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RUN_FINISHED);
    }

    private Map<String, Object> createInfoMessageData(String channel, EventHolder eventHolder) {
        return createDefaultData(channel, eventHolder);
    }

    private Map<String, Object> createMessageData(String channel, EventHolder eventHolder) {
        HashMap<String, Object> section2 = new HashMap<>();
        addOptional(section2, eventHolder);
        HashMap<String, Object> data = createDefaultData(channel, eventHolder);
        data.put("section", section2);
        return data;
    }

    private HashMap<String, Object> createDefaultData(String channel, EventHolder eventHolder) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("channel", channel);
        data.put("description", eventHolder.getCourgetteEvent().getDescription());
        data.put("timestamp", Instant.now().toString());
        data.put("session_id", eventHolder.getCourgetteProperties().getSessionId());
        addIcon(data, eventHolder.getCourgetteEvent());
        return data;
    }

    private void addOptional(Map<String, Object> data, EventHolder eventHolder) {
        CourgetteRunResult courgetteRunResult = eventHolder.getCourgetteRunResult();

        String featureFile = Arrays.stream(courgetteRunResult.getFeatureUri().split(File.separator))
                .reduce((x, y) -> y)
                .orElse(courgetteRunResult.getFeatureUri());

        if (featureFile.contains(":")) {
            Map<String, Object> optional1 = new HashMap<>();

            String[] parts = featureFile.split(":");
            featureFile = parts[0];
            String lineNumber = parts[1];

            optional1.put("scenario", getScenarioName(courgetteRunResult, lineNumber));
            optional1.put("line", lineNumber);

            data.put("feature", trimFeature(featureFile));
            data.put("optional1", optional1);

        } else {
            data.put("feature", trimFeature(featureFile));
        }

        if (eventHolder.getCourgetteProperties().getCourgetteOptions().rerunFailedScenarios()
                && !eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RERUN)) {

            Map<String, Object> optional2 = new HashMap<>();
            optional2.put("rerun", isRerun(courgetteRunResult));
            data.put("optional2", optional2);
        }
    }

    private void addIcon(Map<String, Object> data, CourgetteEvent event) {
        switch (event) {
            case TEST_RUN_STARTED:
            case TEST_RUN_FINISHED:
                data.put("icon", "information_source");
                break;
            case TEST_PASSED:
            case TEST_PASSED_AFTER_RERUN:
                data.put("icon", "white_check_mark");
                break;
            case TEST_FAILED:
                data.put("icon", "x");
                break;
            case TEST_RERUN:
                data.put("icon", "warning");
                break;
        }
    }

    private String trimFeature(String feature) {
        return feature.split("\\.")[0].trim();
    }

    private String getScenarioName(CourgetteRunResult courgetteRunResult, String lineNumber) {
        return courgetteRunResult.getFeature()
                .getPickles()
                .stream().filter(t -> t.getLocation().getLine() == Integer.parseInt(lineNumber))
                .findFirst()
                .get()
                .getName();
    }

    private boolean isRerun(CourgetteRunResult courgetteRunResult) {
        return courgetteRunResult.getStatus().equals(CourgetteRunResult.Status.PASSED_AFTER_RERUN) ||
                courgetteRunResult.getStatus().equals(CourgetteRunResult.Status.FAILED_AFTER_RERUN);
    }

    private String createFromTemplate(Mustache template, Object data) {
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return writer.toString();
    }

    private Mustache readTemplate(String template) {
        StringBuilder templateContent = new StringBuilder();

        try {
            final InputStream in = getClass().getResourceAsStream(template);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                templateContent.append(line);
            }
        } catch (Exception e) {
            printExceptionStackTrace(e);
        }

        return new DefaultMustacheFactory().compile(new StringReader(templateContent.toString()), "");
    }
}
