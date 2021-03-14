package courgette.runtime.event;

import courgette.runtime.event.subscriber.TestFailedEventSubscriber;
import courgette.runtime.event.subscriber.TestFinishedEventSubscriber;
import courgette.runtime.event.subscriber.TestPassedAfterRerunEventSubscriber;
import courgette.runtime.event.subscriber.TestPassedEventSubscriber;
import courgette.runtime.event.subscriber.TestRerunEventSubscriber;
import courgette.runtime.event.subscriber.TestStartedEventSubscriber;

import java.util.List;

public final class EventSubscriberCreator {

    public static void createEventSubscribers(EventPublisher eventPublisher, List<CourgetteEvent> courgetteEvents) {

        if (eventPublisher != null) {
            if (courgetteEvents.stream().anyMatch(e -> e.equals(CourgetteEvent.ALL))) {
                eventPublisher.addEventSubscriber(new TestStartedEventSubscriber());
                eventPublisher.addEventSubscriber(new TestFinishedEventSubscriber());
                eventPublisher.addEventSubscriber(new TestPassedEventSubscriber());
                eventPublisher.addEventSubscriber(new TestFailedEventSubscriber());
                eventPublisher.addEventSubscriber(new TestRerunEventSubscriber());
                eventPublisher.addEventSubscriber(new TestPassedAfterRerunEventSubscriber());
            } else {
                courgetteEvents.forEach(eventType -> {
                    switch (eventType) {
                        case TEST_RUN_STARTED:
                            eventPublisher.addEventSubscriber(new TestStartedEventSubscriber());
                            break;
                        case TEST_RUN_FINISHED:
                            eventPublisher.addEventSubscriber(new TestFinishedEventSubscriber());
                            break;
                        case TEST_PASSED:
                            eventPublisher.addEventSubscriber(new TestPassedEventSubscriber());
                            break;
                        case TEST_PASSED_AFTER_RERUN:
                            eventPublisher.addEventSubscriber(new TestPassedAfterRerunEventSubscriber());
                            break;
                        case TEST_FAILED:
                            eventPublisher.addEventSubscriber(new TestFailedEventSubscriber());
                            break;
                        case TEST_RERUN:
                            eventPublisher.addEventSubscriber(new TestRerunEventSubscriber());
                            break;
                    }
                });
            }
        }
    }
}
