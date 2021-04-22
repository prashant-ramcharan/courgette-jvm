package courgette.runtime.event;

import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunnerInfo;

public class EventHolder {

    private final CourgetteEvent courgetteEvent;
    private final CourgetteProperties courgetteProperties;
    private final CourgetteRunnerInfo courgetteRunnerInfo;
    private final CourgetteRunResult courgetteRunResult;

    public EventHolder(CourgetteEvent courgetteEvent,
                       CourgetteProperties courgetteProperties,
                       CourgetteRunnerInfo courgetteRunnerInfo,
                       CourgetteRunResult courgetteRunResult) {

        this.courgetteEvent = courgetteEvent;
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunnerInfo = courgetteRunnerInfo;
        this.courgetteRunResult = courgetteRunResult;
    }

    public CourgetteEvent getCourgetteEvent() {
        return courgetteEvent;
    }

    public CourgetteProperties getCourgetteProperties() {
        return courgetteProperties;
    }

    public CourgetteRunnerInfo getCourgetteRunnerInfo() {
        return courgetteRunnerInfo;
    }

    public CourgetteRunResult getCourgetteRunResult() {
        return courgetteRunResult;
    }
}
