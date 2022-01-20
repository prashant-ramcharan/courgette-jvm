package courgette.runtime.event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class EventPublisher {

    private final EventSender eventSender;
    private final PropertyChangeSupport support;

    public EventPublisher(EventSender eventSender) {
        this.eventSender = eventSender;
        this.support = new PropertyChangeSupport(this);
    }

    public void addEventSubscriber(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public synchronized void publishEvent(CourgetteEventHolder eventHolder) {
        support.firePropertyChange("eventHolder", null, eventHolder);
        support.firePropertyChange("send", null, eventSender);
    }
}
