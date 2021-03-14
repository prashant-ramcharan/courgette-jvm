package courgette.runtime.event;

import java.beans.PropertyChangeEvent;

public abstract class EventSubscriber {

    protected EventHolder eventHolder;

    protected boolean matchesEvent(CourgetteEvent courgetteEvent) {
        return eventHolder.getCourgetteEvent().equals(courgetteEvent);
    }

    protected abstract void sendEvent(EventSender eventSender);

    protected void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("eventHolder")) {
            this.eventHolder = (EventHolder) evt.getNewValue();
        } else {
            sendEvent((EventSender) evt.getNewValue());
        }
    }
}
