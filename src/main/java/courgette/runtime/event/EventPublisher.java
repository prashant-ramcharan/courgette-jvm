package courgette.runtime.event;

import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunnerInfo;

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

    public void publishEvent(CourgetteEvent courgetteEvent,
                             CourgetteProperties courgetteProperties,
                             CourgetteRunnerInfo courgetteRunnerInfo,
                             CourgetteRunResult courgetteRunResult) {

        final EventHolder eventHolder = new EventHolder(courgetteEvent, courgetteProperties, courgetteRunnerInfo, courgetteRunResult);
        support.firePropertyChange("eventHolder", null, eventHolder);
        support.firePropertyChange("send", null, eventSender);
    }
}
