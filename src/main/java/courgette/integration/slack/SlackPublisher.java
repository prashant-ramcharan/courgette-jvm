package courgette.integration.slack;

import courgette.runtime.event.CourgetteEventHolder;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgettePublisher;
import courgette.runtime.CourgetteSlackOptions;
import courgette.runtime.event.EventPublisher;
import courgette.runtime.event.EventSubscriberCreator;

import java.util.Arrays;
import java.util.Optional;

public class SlackPublisher implements CourgettePublisher {

    private Optional<EventPublisher> eventPublisher = Optional.empty();

    public SlackPublisher(CourgetteProperties courgetteProperties) {

        if (courgetteProperties.publishEventsToSlack()) {
            final CourgetteSlackOptions slackOptions = courgetteProperties.slackOptions();

            final SlackService slackService = new SlackService(slackOptions.getWebhookUrl());

            eventPublisher = Optional.of(new EventPublisher(new SlackMessageSender(slackService, slackOptions)));

            EventSubscriberCreator.createEventSubscribers(eventPublisher.get(), Arrays.asList(courgetteProperties.getCourgetteOptions().slackEventSubscription()));
        }
    }

    @Override
    public void publish(CourgetteEventHolder eventHolder) {
        eventPublisher.ifPresent(p -> p.publishEvent(eventHolder));
    }
}
