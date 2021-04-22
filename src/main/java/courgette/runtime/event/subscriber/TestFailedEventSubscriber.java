package courgette.runtime.event.subscriber;

import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.event.EventSender;
import courgette.runtime.event.EventSubscriber;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TestFailedEventSubscriber extends EventSubscriber implements PropertyChangeListener {

    @Override
    public void sendEvent(EventSender eventSender) {
        if (matchesEvent(CourgetteEvent.TEST_FAILED)) {
            eventSender.send(eventHolder);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }
}
