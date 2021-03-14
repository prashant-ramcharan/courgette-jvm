package courgette.runtime.event.subscriber;

import courgette.runtime.event.EventSender;
import courgette.runtime.event.EventSubscriber;
import courgette.runtime.event.CourgetteEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TestPassedEventSubscriber extends EventSubscriber implements PropertyChangeListener {

    @Override
    public void sendEvent(EventSender eventSender) {
        if (matchesEvent(CourgetteEvent.TEST_PASSED)) {
            eventSender.send(eventHolder);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }
}
