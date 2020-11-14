package courgette.runtime;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.messages.Messages;
import io.cucumber.messages.NdjsonToMessageIterable;
import io.cucumber.messages.internal.com.google.protobuf.util.JsonFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static java.util.Comparator.comparingLong;

public class CourgetteNdJsonCreator {

    private Map<Feature, CopyOnWriteArrayList<String>> ndJsonData;

    public CourgetteNdJsonCreator() {
    }

    public CourgetteNdJsonCreator(Map<Feature, CopyOnWriteArrayList<String>> ndJsonData) {
        this.ndJsonData = ndJsonData;
    }

    public List<Messages.Envelope> createFeatureMessages() {
        return getFeatureMessages();
    }

    public List<Messages.Envelope> createScenarioMessages() {
        return getScenarioMessages();
    }

    public String toNdJsonMessageString(List<Messages.Envelope> envelopes) {
        final StringWriter writer = new StringWriter();

        final JsonFormat.Printer jsonPrinter = JsonFormat.printer().omittingInsignificantWhitespace();

        envelopes.forEach(envelope -> {
            try {
                jsonPrinter.appendTo(envelope, writer);
                writer.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return writer.toString();
    }

    private List<Messages.Envelope> getFeatureMessages() {
        List<Messages.Envelope> featureMessages = new ArrayList<>();

        for (CopyOnWriteArrayList<String> messages : ndJsonData.values()) {
            messages.forEach(message -> {
                InputStream messageStream = new ByteArrayInputStream(message.getBytes());
                new NdjsonToMessageIterable(messageStream).forEach(featureMessages::add);
            });
        }

        return mutateMessages(featureMessages);
    }

    private List<Messages.Envelope> getScenarioMessages() {
        List<Messages.Envelope> scenarioMessages = new ArrayList<>();

        ndJsonData.forEach((key, value) -> {

            List<Messages.GherkinDocument.Feature.Scenario.Builder> scenarios = new ArrayList<>();

            List<Messages.Envelope> messages = new ArrayList<>();

            getReportMessages(value).forEach(message -> {
                List<Messages.Envelope> envelopes = new ArrayList<>();

                InputStream messageStream = new ByteArrayInputStream(message.getBytes());
                new NdjsonToMessageIterable(messageStream).forEach(envelopes::add);

                addMessage(envelopes, scenarios, messages);
            });

            Messages.Envelope oldGherkinDocument = extractFirstGherkinDocument(messages);
            Messages.Envelope newGherkinDocument = createNewGherkinDocument(oldGherkinDocument, scenarios);

            messages.removeIf(gherkinEnvelope);
            messages.add(3, newGherkinDocument);

            scenarioMessages.addAll(messages);
        });

        return mutateMessages(scenarioMessages);
    }

    private List<Messages.Envelope> mutateMessages(List<Messages.Envelope> envelopes) {
        Messages.Envelope testRunStarted = createTestRunStarted(envelopes);
        Messages.Envelope testRunFinished = createTestRunFinished(envelopes);

        envelopes.subList(1, envelopes.size()).removeIf(metaEnvelope);
        envelopes.removeIf(testRunStartedOrFinishedEnvelope);

        envelopes.add(2, testRunStarted);
        envelopes.add(testRunFinished);

        return envelopes;
    }

    private List<String> getReportMessages(CopyOnWriteArrayList<String> messages) {
        return new ArrayList<>(messages);
    }

    private void addMessage(List<Messages.Envelope> envelopes,
                            List<Messages.GherkinDocument.Feature.Scenario.Builder> scenarios,
                            List<Messages.Envelope> messages) {

        final List<Messages.Envelope> envelopeList = new ArrayList<>();

        final String testCaseId = extractTestCaseId(envelopes);

        final Messages.Pickle pickle = extractPickle(envelopes, testCaseId);

        envelopes.forEach(envelope -> {

            if (Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase())) {

                final Messages.GherkinDocument.Feature.Scenario.Builder scenario = envelope.toBuilder()
                        .getGherkinDocument()
                        .getFeatureOrBuilder()
                        .getChildrenOrBuilderList()
                        .stream()
                        .map(scenarioBuilder -> scenarioBuilder.getScenario().toBuilder())
                        .filter(scenarioBuilder -> scenarioBuilder.getId().equals(pickle.getAstNodeIds(0)))
                        .findFirst()
                        .get();

                scenarios.add(scenario);
                envelopeList.add(envelope);

            } else if (Messages.Envelope.MessageCase.PICKLE.equals(envelope.getMessageCase())) {
                if (envelope.getPickle() == pickle) {
                    envelopeList.add(envelope);
                }
            } else {
                envelopeList.add(envelope);
            }
        });

        messages.addAll(envelopeList);
    }

    private Messages.Envelope extractFirstGherkinDocument(List<Messages.Envelope> envelopes) {
        return envelopes.stream()
                .filter(envelope -> Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase()))
                .filter(envelope -> !envelope.getGherkinDocument().getUri().equals(""))
                .findFirst()
                .get();
    }

    private Messages.Envelope createNewGherkinDocument(Messages.Envelope gherkinDocument,
                                                       List<Messages.GherkinDocument.Feature.Scenario.Builder> scenarios) {

        final List<Messages.GherkinDocument.Feature.FeatureChild> children = new ArrayList<>();

        scenarios.forEach(scenario -> {
            Messages.GherkinDocument.Feature.FeatureChild.Builder fcb = Messages.GherkinDocument.Feature.FeatureChild.newBuilder();
            fcb.setScenario(scenario);
            children.add(fcb.build());
        });

        final Messages.GherkinDocument.Feature feature = gherkinDocument.getGherkinDocument()
                .getFeature().toBuilder()
                .clearChildren()
                .addAllChildren(children)
                .build();

        return gherkinDocument.toBuilder().setGherkinDocument(
                gherkinDocument.getGherkinDocument()
                        .toBuilder()
                        .setFeature(feature))
                .build();
    }

    private String extractTestCaseId(List<Messages.Envelope> envelopes) {
        return envelopes.stream()
                .filter(envelope -> !envelope.getTestCase().getPickleId().equals(""))
                .map(Messages.Envelope::getTestCase)
                .findFirst()
                .get()
                .getPickleId();
    }

    private Messages.Pickle extractPickle(List<Messages.Envelope> envelopes, String testCaseId) {
        return envelopes.stream()
                .filter(pickle -> pickle.getPickle().getId().equals(testCaseId))
                .map(Messages.Envelope::getPickle)
                .findFirst()
                .get();
    }

    private Messages.Envelope createTestRunStarted(List<Messages.Envelope> envelopes) {

        long seconds = envelopes.stream()
                .filter(envelope -> Messages.Envelope.MessageCase.TEST_RUN_STARTED.equals(envelope.getMessageCase()))
                .map(Messages.Envelope::getTestRunStarted)
                .map(Messages.TestRunStarted::getTimestamp)
                .min(comparingLong(Messages.Timestamp::getSeconds))
                .get().getSeconds();

        Messages.Timestamp timestamp = Messages.Timestamp.newBuilder()
                .setSeconds(seconds)
                .build();

        return Messages.Envelope.newBuilder().setTestRunStarted(
                Messages.TestRunStarted.newBuilder().setTimestamp(timestamp)
        ).build();
    }

    private Messages.Envelope createTestRunFinished(List<Messages.Envelope> envelopes) {

        long seconds = envelopes.stream()
                .filter(envelope -> Messages.Envelope.MessageCase.TEST_RUN_FINISHED.equals(envelope.getMessageCase()))
                .map(Messages.Envelope::getTestRunFinished)
                .map(Messages.TestRunFinished::getTimestamp)
                .max(comparingLong(Messages.Timestamp::getSeconds))
                .get().getSeconds();

        Messages.Timestamp timestamp = Messages.Timestamp.newBuilder()
                .setSeconds(seconds)
                .build();

        return Messages.Envelope.newBuilder().setTestRunFinished(
                Messages.TestRunFinished.newBuilder().setTimestamp(timestamp).build()
        ).build();
    }

    private Predicate<Messages.Envelope> gherkinEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase());

    private Predicate<Messages.Envelope> metaEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.META.equals(envelope.getMessageCase());

    private Predicate<Messages.Envelope> testRunStartedOrFinishedEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.TEST_RUN_STARTED.equals(envelope.getMessageCase())
                    || Messages.Envelope.MessageCase.TEST_RUN_FINISHED.equals(envelope.getMessageCase());
}
