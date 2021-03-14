package courgette.runtime.event;

import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;

public class EventHolder {

    private final CourgetteEvent courgetteEvent;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRunResult courgetteRunResult;

    public EventHolder(CourgetteEvent courgetteEvent,
                       CourgetteProperties courgetteProperties,
                       CourgetteRunResult courgetteRunResult) {
        this.courgetteEvent = courgetteEvent;
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunResult = courgetteRunResult;
    }

    public CourgetteEvent getCourgetteEvent() {
        return courgetteEvent;
    }

    public CourgetteProperties getCourgetteProperties() {
        return courgetteProperties;
    }

    public CourgetteRunResult getCourgetteRunResult() {
        return courgetteRunResult;
    }
}
