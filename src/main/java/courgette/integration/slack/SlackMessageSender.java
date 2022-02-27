package courgette.integration.slack;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import courgette.runtime.event.CourgetteEventHolder;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteSlackOptions;
import courgette.runtime.CourgetteTestStatistics;
import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.event.EventSender;
import io.cucumber.core.gherkin.Feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static courgette.runtime.CourgetteException.printError;
import static courgette.runtime.CourgetteException.printExceptionStackTrace;

public class SlackMessageSender implements EventSender {

    private final SlackService slackService;
    private final CourgetteSlackOptions slackOptions;
    private final Mustache messageTemplate;
    private final Mustache summaryTemplate;

    public SlackMessageSender(SlackService slackService, CourgetteSlackOptions slackOptions) {
        this.slackService = slackService;
        this.slackOptions = slackOptions;
        this.messageTemplate = readTemplate("/slack/message.mustache");
        this.summaryTemplate = readTemplate("/slack/summary.mustache");
    }

    @Override
    public void send(CourgetteEventHolder eventHolder) {
        slackOptions.getChannels().forEach(channel -> createMessage(channel, eventHolder).ifPresent(slackService::postMessage));
    }

    private Optional<String> createMessage(String channel, CourgetteEventHolder eventHolder) {
        try {
            if (isTestRunSummaryEvent((eventHolder))) {
                return Optional.of(createFromTemplate(summaryTemplate, createSummaryMessageData(channel, eventHolder)));
            }
            if (isInfoEvent(eventHolder)) {
                return Optional.of(createFromTemplate(messageTemplate, createInfoMessageData(channel, eventHolder)));
            } else {
                return Optional.of(createFromTemplate(messageTemplate, createMessageData(channel, eventHolder)));
            }
        } catch (Exception e) {
            printError("Courgette Slack Message: There was an error creating the slack message -> " + e.getMessage());
        }

        return Optional.empty();
    }

    private boolean isInfoEvent(CourgetteEventHolder eventHolder) {
        return eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RUN_STARTED)
                || eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RUN_FINISHED);
    }

    private boolean isTestRunSummaryEvent(CourgetteEventHolder eventHolder) {
        return eventHolder.getCourgetteEvent().equals(CourgetteEvent.TEST_RUN_SUMMARY);
    }

    private Map<String, Object> createInfoMessageData(String channel, CourgetteEventHolder eventHolder) {
        return createDefaultData(channel, eventHolder);
    }

    private Map<String, Object> createMessageData(String channel, CourgetteEventHolder eventHolder) {
        HashMap<String, Object> section = new HashMap<>();
        addOptional(section, eventHolder);
        HashMap<String, Object> data = createDefaultData(channel, eventHolder);
        data.put("section", section);
        return data;
    }

    private Map<String, Object> createSummaryMessageData(String channel, CourgetteEventHolder eventHolder) {
        CourgetteTestStatistics testStatistics = eventHolder.getCourgetteTestStatistics();

        HashMap<String, Object> data = createDefaultData(channel, eventHolder);
        data.put("run_level", eventHolder.getCourgetteProperties().getCourgetteOptions().runLevel().toString());
        data.put("total", testStatistics.total());
        data.put("passed", testStatistics.passed());
        data.put("passed_percentage", testStatistics.passedPercentage());
        data.put("failed", testStatistics.failed());
        data.put("failed_percentage", testStatistics.failedPercentage());
        data.put("duration", testStatistics.duration());

        if (eventHolder.getCourgetteProperties().getCourgetteOptions().rerunFailedScenarios()
                && testStatistics.rerun() > 0) {
            Map<String, Object> optional1 = new HashMap<>();
            optional1.put("rerun", testStatistics.rerun());
            data.put("optional1", optional1);

            if (testStatistics.passedAfterRerun() > 0) {
                Map<String, Object> optional2 = new HashMap<>();
                optional2.put("passed_rerun", testStatistics.passedAfterRerun());
                data.put("optional2", optional2);
            }
        }
        return data;
    }

    private HashMap<String, Object> createDefaultData(String channel, CourgetteEventHolder eventHolder) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("channel", channel);
        data.put("description", eventHolder.getCourgetteEvent().getDescription());
        data.put("timestamp", Instant.now().toString());
        data.put("session_id", eventHolder.getCourgetteProperties().getSessionId());
        addTestId(data, slackOptions.getTestId());
        addIcon(data, eventHolder.getCourgetteEvent());
        return data;
    }

    private void addTestId(Map<String, Object> data, String testId) {
        if (!testId.trim().isEmpty()) {
            HashMap<String, Object> testIdData = new HashMap<>();
            testIdData.put("test_id", testId);
            data.put("testId", testIdData);
        }
    }

    private void addOptional(Map<String, Object> data, CourgetteEventHolder eventHolder) {
        CourgetteRunResult courgetteRunResult = eventHolder.getCourgetteRunResult();

        data.put("feature", trimFeatureName(courgetteRunResult.getFeature()));

        if (courgetteRunResult.getLineId() != null) {
            Map<String, Object> optional1 = new HashMap<>();
            optional1.put("scenario", getScenarioName(courgetteRunResult, courgetteRunResult.getLineId()));
            optional1.put("line", courgetteRunResult.getLineId());
            data.put("optional1", optional1);
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
            case TEST_RUN_SUMMARY:
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

    private String trimFeatureName(Feature feature) {
        return new File(feature.getUri()).getName().split("\\.")[0].trim();
    }

    private String getScenarioName(CourgetteRunResult courgetteRunResult, Integer lineNumber) {
        return courgetteRunResult.getFeature()
                .getPickles()
                .stream().filter(t -> t.getLocation().getLine() == lineNumber)
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
