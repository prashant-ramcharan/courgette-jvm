package courgette.integration.slack;

import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgettePublisher;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteSlackOptions;
import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.event.EventPublisher;
import courgette.runtime.event.EventSubscriberCreator;

import java.util.Arrays;
import java.util.Optional;

public class SlackPublisher implements CourgettePublisher {

    private final CourgetteProperties courgetteProperties;

    private Optional<EventPublisher> eventPublisher = Optional.empty();

    public SlackPublisher(CourgetteProperties courgetteProperties) {

        this.courgetteProperties = courgetteProperties;

        if (courgetteProperties.publishEventsToSlack()) {
            final CourgetteSlackOptions slackOptions = courgetteProperties.slackOptions();

            final SlackService slackService = new SlackService(slackOptions.getWebhookUrl());

            eventPublisher = Optional.of(new EventPublisher(new SlackMessageSender(slackService, slackOptions)));

            EventSubscriberCreator.createEventSubscribers(eventPublisher.get(), Arrays.asList(courgetteProperties.getCourgetteOptions().slackEventSubscription()));
        }
    }

    @Override
    public void publish(CourgetteEvent event) {
        eventPublisher.ifPresent(p -> p.publishEvent(event, courgetteProperties, null, null));
    }

    @Override
    public void publish(CourgetteEvent event, CourgetteRunResult result) {
        eventPublisher.ifPresent(p -> p.publishEvent(event, courgetteProperties, null, result));
    }

    @Override
    public void publish(CourgetteEvent event, CourgetteRunnerInfo runnerInfo, CourgetteRunResult result) {
        eventPublisher.ifPresent(p -> p.publishEvent(event, courgetteProperties, runnerInfo, result));
    }
}
