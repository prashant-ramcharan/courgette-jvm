package courgette.runtime;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.messages.Messages;
import io.cucumber.messages.NdjsonToMessageIterable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static java.util.Comparator.comparingLong;

public class CourgetteNdJsonCreator {

    private final Map<Feature, List<List<Messages.Envelope>>> messages;

    public CourgetteNdJsonCreator(Map<Feature, List<List<Messages.Envelope>>> messages) {
        this.messages = messages;
    }

    public static List<Messages.Envelope> createMessages(String source) {

        List<String> messageList = Arrays.asList(source.split("\n"));

        List<Messages.Envelope> messages = new ArrayList<>(messageList.size());

        messageList.forEach(m -> {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(m.getBytes(StandardCharsets.UTF_8));

                for (Messages.Envelope message : new NdjsonToMessageIterable(in)) {
                    messages.add(message);
                }

            } catch (Exception e) {
                printExceptionStackTrace(e);
            }
        });

        return messages;
    }

    public List<Messages.Envelope> createFeatureMessages() {
        return getMessages();
    }

    public List<Messages.Envelope> createScenarioMessages() {
        return getScenarioMessages();
    }

    private List<Messages.Envelope> getMessages() {
        List<Messages.Envelope> featureMessages =
                this.messages.values().stream()
                        .flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        return mutateMessages(featureMessages);
    }

    private List<Messages.Envelope> getScenarioMessages() {
        List<Messages.Envelope> scenarioMessages = new ArrayList<>();

        messages.forEach((k, v) -> {

            List<Messages.GherkinDocument.Feature.Scenario.Builder> scenarios = new ArrayList<>();

            List<Messages.Envelope> messages = new ArrayList<>();

            v.forEach(message -> addMessage(message, scenarios, messages));

            Messages.Envelope oldGherkinDocument = extractFirstGherkinDocument(messages);
            Messages.Envelope newGherkinDocument = createNewGherkinDocument(oldGherkinDocument, scenarios);

            messages.removeIf(gherkinEnvelope);
            messages.add(3, newGherkinDocument);

            scenarioMessages.addAll(messages);
        });

        return mutateMessages(scenarioMessages);
    }

    private List<Messages.Envelope> mutateMessages(List<Messages.Envelope> envelopes) {

        if (envelopes != null && !envelopes.isEmpty()) {
            Messages.Envelope testRunStarted = createTestRunStarted(envelopes);
            Messages.Envelope testRunFinished = createTestRunFinished(envelopes);

            envelopes.subList(1, envelopes.size()).removeIf(metaEnvelope);
            envelopes.removeIf(testRunStartedOrFinishedEnvelope);

            envelopes.add(2, testRunStarted);
            envelopes.add(testRunFinished);
            return envelopes;
        }

        return null;
    }

    private void addMessage(List<Messages.Envelope> envelopes,
                            List<Messages.GherkinDocument.Feature.Scenario.Builder> scenarios,
                            List<Messages.Envelope> messages) {

        final List<Messages.Envelope> envelopeList = new ArrayList<>();

        final Optional<Messages.TestCase> testCase = extractTestCase(envelopes);

        final Optional<Messages.Pickle> pickle = extractPickle(envelopes, testCase);

        envelopes.forEach(envelope -> {

            if (pickle.isPresent()) {
                if (Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase())) {

                    final Optional<Messages.GherkinDocument.Feature.Scenario.Builder> scenario = envelope.toBuilder()
                            .getGherkinDocument()
                            .getFeatureOrBuilder()
                            .getChildrenOrBuilderList()
                            .stream()
                            .map(scenarioBuilder -> scenarioBuilder.getScenario().toBuilder())
                            .filter(scenarioBuilder -> scenarioBuilder.getId().equals(pickle.get().getAstNodeIds(0)))
                            .findFirst();

                    scenario.ifPresent(scenarios::add);

                    envelopeList.add(envelope);

                } else if (Messages.Envelope.MessageCase.PICKLE.equals(envelope.getMessageCase())) {
                    if (envelope.getPickle() == pickle.get()) {
                        envelopeList.add(envelope);
                    }
                } else {
                    envelopeList.add(envelope);
                }
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

    private Optional<Messages.TestCase> extractTestCase(List<Messages.Envelope> envelopes) {
        return envelopes.stream()
                .map(Messages.Envelope::getTestCase)
                .filter(testCase -> !testCase.getPickleId().equals(""))
                .findFirst();
    }

    private Optional<Messages.Pickle> extractPickle(List<Messages.Envelope> envelopes, Optional<Messages.TestCase> testCase) {
        if (!envelopes.isEmpty() && testCase.isPresent()) {
            return envelopes.stream()
                    .map(Messages.Envelope::getPickle)
                    .filter(pickle -> pickle.getId().equals(testCase.get().getPickleId()))
                    .findFirst();
        }
        return Optional.empty();
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

    private final Predicate<Messages.Envelope> gherkinEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.GHERKIN_DOCUMENT.equals(envelope.getMessageCase());

    private final Predicate<Messages.Envelope> metaEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.META.equals(envelope.getMessageCase());

    private final Predicate<Messages.Envelope> testRunStartedOrFinishedEnvelope = (envelope) ->
            Messages.Envelope.MessageCase.TEST_RUN_STARTED.equals(envelope.getMessageCase())
                    || Messages.Envelope.MessageCase.TEST_RUN_FINISHED.equals(envelope.getMessageCase());
}
