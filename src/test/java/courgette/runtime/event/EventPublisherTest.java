package courgette.runtime.event;

import org.junit.Test;

import courgette.runtime.CourgetteProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventPublisherTest {
    @Test
    public void sendsOnlySubscribedEvents() {
        RecordingSender sender = new RecordingSender();
        EventPublisher publisher = new EventPublisher(sender);
        EventSubscriberCreator.createEventSubscribers(publisher, Arrays.asList(CourgetteEvent.TEST_RUN_STARTED, CourgetteEvent.TEST_FAILED));

        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_RUN_STARTED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_PASSED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_FAILED, (CourgetteProperties) null));

        assertEquals(Arrays.asList(CourgetteEvent.TEST_RUN_STARTED, CourgetteEvent.TEST_FAILED), sender.events);
    }

    @Test
    public void allSubscriptionSendsEveryConcreteEvent() {
        RecordingSender sender = new RecordingSender();
        EventPublisher publisher = new EventPublisher(sender);
        EventSubscriberCreator.createEventSubscribers(publisher, java.util.Collections.singletonList(CourgetteEvent.ALL));

        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_RUN_STARTED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_RUN_FINISHED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_PASSED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_FAILED, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_RERUN, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_PASSED_AFTER_RERUN, (CourgetteProperties) null));
        publisher.publishEvent(new CourgetteEventHolder(CourgetteEvent.TEST_RUN_SUMMARY, (CourgetteProperties) null));

        assertEquals(7, sender.events.size());
    }

    private static class RecordingSender implements EventSender {
        private final List<CourgetteEvent> events = new ArrayList<>();

        @Override
        public void send(CourgetteEventHolder eventHolder) {
            events.add(eventHolder.getCourgetteEvent());
        }
    }
}
