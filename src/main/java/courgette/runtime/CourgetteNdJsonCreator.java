package courgette.runtime;

import io.cucumber.messages.NdjsonToMessageIterable;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.TestCase;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import io.cucumber.messages.types.Timestamp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

import static courgette.runtime.utils.JacksonUtils.CUCUMBER_OBJECT_MAPPER;
import static java.util.Comparator.comparingLong;

public class CourgetteNdJsonCreator {
    private final Map<String, List<List<Envelope>>> messages;

    public CourgetteNdJsonCreator(Map<String, List<List<Envelope>>> messages) {
        this.messages = messages;
    }

    public static List<Envelope> createMessages(String source) {
        final List<String> messageList = Arrays.asList(source.split("\n"));

        final List<Envelope> messages = new ArrayList<>(messageList.size());

        messageList.forEach(message -> {
            ByteArrayInputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
            for (Envelope envelope : new NdjsonToMessageIterable(in, new NdJsonMessageDeserializer())) {
                messages.add(envelope);
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
                if (envelope.getGherkinDocument().isPresent() &&
                        envelope.getGherkinDocument().get().getFeature().isPresent()) {
                    final Optional<Scenario> scenario = envelope.getGherkinDocument().get().getFeature().get().getChildren()
                            .stream()
                            .map(FeatureChild::getScenario)
                            .filter(Objects::nonNull)
                            .filter(s -> s.isPresent() && s.get().getId().equals(pickle.get().getAstNodeIds().get(0)))
                            .findFirst()
                            .orElse(Optional.empty());

                    scenario.ifPresent(scenarios::add);

                    envelopeList.add(envelope);

                } else if (envelope.getPickle().isPresent()) {
                    if (envelope.getPickle().get() == pickle.get()) {
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

    private Envelope createNewGherkinDocument(Envelope envelope, List<Scenario> scenarios) {
        final List<FeatureChild> children = new ArrayList<>();
        scenarios.forEach(scenario -> children.add(FeatureChild.of(scenario)));

        final io.cucumber.messages.types.Feature currentFeature = envelope.getGherkinDocument().flatMap(GherkinDocument::getFeature).get();

        final io.cucumber.messages.types.Feature newFeature = new io.cucumber.messages.types.Feature(
                currentFeature.getLocation(),
                currentFeature.getTags(),
                currentFeature.getLanguage(),
                currentFeature.getKeyword(),
                currentFeature.getName(),
                currentFeature.getDescription(),
                children
        );

        GherkinDocument currentGherkinDoc = envelope.getGherkinDocument().get();
        GherkinDocument newGherkinDoc = new GherkinDocument(currentGherkinDoc.getUri().get(), newFeature, currentGherkinDoc.getComments());

        return Envelope.of(newGherkinDoc);
    }

    private Optional<TestCase> extractTestCase(List<Envelope> envelopes) {
        return envelopes.stream()
                .map(Envelope::getTestCase)
                .filter(Objects::nonNull)
                .filter(testCase)
                .findFirst()
                .get();
    }

    private Optional<Pickle> extractPickle(List<Envelope> envelopes, Optional<TestCase> testCase) {
        if (!envelopes.isEmpty() && testCase.isPresent()) {
            return envelopes.stream()
                    .map(Envelope::getPickle)
                    .filter(Objects::nonNull)
                    .filter(pickle -> pickle.isPresent() && pickle.get().getId().equals(testCase.get().getPickleId()))
                    .findFirst()
                    .orElse(Optional.empty());
        }
        return Optional.empty();
    }

    private Envelope createTestRunStarted(List<Envelope> envelopes) {
        Timestamp timestamp = envelopes.stream()
                .filter(envelope -> envelope.getTestRunStarted().isPresent())
                .map(envelope -> envelope.getTestRunStarted().get().getTimestamp())
                .min(comparingLong(Timestamp::getSeconds))
                .get();

        return Envelope.of(new TestRunStarted(timestamp));
    }

    private Envelope createTestRunFinished(List<Envelope> envelopes) {
        Timestamp timestamp = envelopes.stream()
                .filter(envelope -> envelope.getTestRunFinished().isPresent())
                .map(envelope -> envelope.getTestRunFinished().get().getTimestamp())
                .min(comparingLong(Timestamp::getSeconds))
                .get();

        return Envelope.of(new TestRunFinished(null, true, timestamp, null));
    }

    private final Predicate<Envelope> gherkinEnvelope = (envelope) -> envelope.getGherkinDocument().isPresent();

    private final Predicate<Envelope> metaEnvelope = (envelope) -> envelope.getMeta().isPresent();

    private final Predicate<Envelope> testRunStartedOrFinishedEnvelope = (envelope) -> envelope.getTestRunStarted().isPresent() || envelope.getTestRunFinished().isPresent();

    private final Predicate<Optional<TestCase>> testCase = (testCase) -> testCase.isPresent() && !testCase.get().getPickleId().equals("");

    private static class NdJsonMessageDeserializer implements NdjsonToMessageIterable.Deserializer {
        @Override
        public Envelope readValue(String json) throws IOException {
            return CUCUMBER_OBJECT_MAPPER.readValue(json, Envelope.class);
        }
    }
}
