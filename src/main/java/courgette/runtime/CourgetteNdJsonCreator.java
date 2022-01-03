package courgette.runtime;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.messages.NdjsonToMessageIterable;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.TestCase;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import io.cucumber.messages.types.Timestamp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static java.util.Comparator.comparingLong;

public class CourgetteNdJsonCreator {

    private final Map<Feature, List<List<Envelope>>> messages;

    public CourgetteNdJsonCreator(Map<Feature, List<List<Envelope>>> messages) {
        this.messages = messages;
    }

    public static List<Envelope> createMessages(String source) {
        final List<String> messageList = Arrays.asList(source.split("\n"));

        final List<Envelope> messages = new ArrayList<>(messageList.size());

        messageList.forEach(m -> {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(m.getBytes(StandardCharsets.UTF_8));

                for (Envelope message : new NdjsonToMessageIterable(in)) {
                    messages.add(message);
                }
            } catch (Exception e) {
                printExceptionStackTrace(e);
            }
        });

        return messages;
    }

    public List<Envelope> createFeatureMessages() {
        return getMessages();
    }

    public List<Envelope> createScenarioMessages() {
        return getScenarioMessages();
    }

    private List<Envelope> getMessages() {
        List<Envelope> featureMessages =
                this.messages.values().stream()
                        .flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        return mutateMessages(featureMessages);
    }

    private List<Envelope> getScenarioMessages() {
        List<Envelope> scenarioMessages = new ArrayList<>();

        messages.forEach((k, v) -> {
            List<Scenario> scenarios = new ArrayList<>();

            List<Envelope> messages = new ArrayList<>();

            v.forEach(message -> addMessage(message, scenarios, messages));

            Envelope oldGherkinDocument = extractFirstGherkinDocument(messages);
            Envelope newGherkinDocument = createNewGherkinDocument(oldGherkinDocument, scenarios);

            messages.removeIf(gherkinEnvelope);
            messages.add(3, newGherkinDocument);

            scenarioMessages.addAll(messages);
        });

        return mutateMessages(scenarioMessages);
    }

    private List<Envelope> mutateMessages(List<Envelope> envelopes) {
        if (envelopes != null && !envelopes.isEmpty()) {
            Envelope testRunStarted = createTestRunStarted(envelopes);
            Envelope testRunFinished = createTestRunFinished(envelopes);

            envelopes.subList(1, envelopes.size()).removeIf(metaEnvelope);
            envelopes.removeIf(testRunStartedOrFinishedEnvelope);

            envelopes.add(2, testRunStarted);
            envelopes.add(testRunFinished);
            return envelopes;
        }

        return null;
    }

    private void addMessage(List<Envelope> envelopes, List<Scenario> scenarios, List<Envelope> messages) {
        final List<Envelope> envelopeList = new ArrayList<>();

        final Optional<TestCase> testCase = extractTestCase(envelopes);

        final Optional<Pickle> pickle = extractPickle(envelopes, testCase);

        envelopes.forEach(envelope -> {
            if (pickle.isPresent()) {
                if (envelope.getGherkinDocument() != null) {
                    final Optional<Scenario> scenario = envelope.getGherkinDocument().getFeature().getChildren()
                            .stream()
                            .map(FeatureChild::getScenario)
                            .filter(Objects::nonNull)
                            .filter(t -> t.getId().equals(pickle.get().getAstNodeIds().get(0)))
                            .findFirst();

                    scenario.ifPresent(scenarios::add);

                    envelopeList.add(envelope);

                } else if (envelope.getPickle() != null) {
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

    private Envelope extractFirstGherkinDocument(List<Envelope> envelopes) {
        return envelopes.stream()
                .filter(gherkinEnvelope)
                .findFirst()
                .get();
    }

    private Envelope createNewGherkinDocument(Envelope gherkinDocument, List<Scenario> scenarios) {
        final List<FeatureChild> children = new ArrayList<>();

        scenarios.forEach(scenario -> {
            FeatureChild featureChild = new FeatureChild();
            featureChild.setScenario((scenario));
            children.add(featureChild);
        });

        final io.cucumber.messages.types.Feature feature = gherkinDocument.getGherkinDocument().getFeature();
        feature.setChildren(children);

        gherkinDocument.getGherkinDocument().setFeature(feature);

        return gherkinDocument;
    }

    private Optional<TestCase> extractTestCase(List<Envelope> envelopes) {
        return envelopes.stream()
                .map(Envelope::getTestCase)
                .filter(Objects::nonNull)
                .filter(testCase)
                .findFirst();
    }

    private Optional<Pickle> extractPickle(List<Envelope> envelopes, Optional<TestCase> testCase) {
        if (!envelopes.isEmpty() && testCase.isPresent()) {
            return envelopes.stream()
                    .map(Envelope::getPickle)
                    .filter(Objects::nonNull)
                    .filter(pickle -> pickle.getId().equals(testCase.get().getPickleId()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Envelope createTestRunStarted(List<Envelope> envelopes) {
        long seconds = envelopes.stream()
                .map(Envelope::getTestRunStarted)
                .filter(Objects::nonNull)
                .map(TestRunStarted::getTimestamp)
                .min(comparingLong(Timestamp::getSeconds))
                .get().getSeconds();

        Timestamp timestamp = new Timestamp();
        timestamp.setSeconds(seconds);

        TestRunStarted testRunStarted = new TestRunStarted();
        testRunStarted.setTimestamp(timestamp);

        Envelope testRunStartedEnvelope = new Envelope();
        testRunStartedEnvelope.setTestRunStarted(testRunStarted);

        return testRunStartedEnvelope;
    }

    private Envelope createTestRunFinished(List<Envelope> envelopes) {
        long seconds = envelopes.stream()
                .map(Envelope::getTestRunFinished)
                .filter(Objects::nonNull)
                .map(TestRunFinished::getTimestamp)
                .max(comparingLong(Timestamp::getSeconds))
                .get().getSeconds();

        Timestamp timestamp = new Timestamp();
        timestamp.setSeconds(seconds);

        TestRunFinished testRunFinished = new TestRunFinished();
        testRunFinished.setTimestamp(timestamp);

        Envelope testRunFinishedEnvelope = new Envelope();
        testRunFinishedEnvelope.setTestRunFinished(testRunFinished);

        return testRunFinishedEnvelope;
    }

    private final Predicate<Envelope> gherkinEnvelope = (envelope) -> envelope.getGherkinDocument() != null;

    private final Predicate<Envelope> metaEnvelope = (envelope) -> envelope.getMeta() != null;

    private final Predicate<Envelope> testRunStartedOrFinishedEnvelope = (envelope) -> envelope.getTestRunStarted() != null || envelope.getTestRunFinished() != null;

    private final Predicate<TestCase> testCase = (testCase) -> !testCase.getPickleId().equals("");
}
