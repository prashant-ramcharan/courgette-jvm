package courgette.runtime;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.messages.MessageToNdjsonWriter;
import io.cucumber.messages.Messages;
import io.cucumber.messages.NdjsonToMessageIterable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
        final ByteArrayOutputStream writer = new ByteArrayOutputStream();

        envelopes.forEach(envelope -> {
            try {
                new MessageToNdjsonWriter(writer).write(envelope);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return writer.toString();
    }

    private List<Messages.Envelope> getFeatureMessages() {
        List<Messages.Envelope> featureEnvelopes = new ArrayList<>();

        for (CopyOnWriteArrayList<String> messages : ndJsonData.values()) {
            messages.forEach(message -> {
                InputStream messageStream = new ByteArrayInputStream(message.getBytes());
                new NdjsonToMessageIterable(messageStream).forEach(featureEnvelopes::add);
            });
        }

        return featureEnvelopes;
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

            messages.removeIf(envelope -> Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase()));
            messages.add(3, newGherkinDocument);

            scenarioMessages.addAll(messages);
        });
        return scenarioMessages;
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

    private String extractTestCaseId(List<Messages.Envelope> messageEnvelope) {
        return messageEnvelope.stream()
                .filter(envelope -> !envelope.getTestCase().getPickleId().equals(""))
                .map(Messages.Envelope::getTestCase)
                .findFirst()
                .get()
                .getPickleId();
    }

    private Messages.Pickle extractPickle(List<Messages.Envelope> messageEnvelope, String testCaseId) {
        return messageEnvelope.stream()
                .filter(pickle -> pickle.getPickle().getId().equals(testCaseId))
                .map(Messages.Envelope::getPickle)
                .findFirst()
                .get();
    }
}
