package courgette.runtime.event;

import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunResult;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.CourgetteTestStatistics;

public class CourgetteEventHolder {
    private CourgetteEvent courgetteEvent;
    private CourgetteProperties courgetteProperties;
    private CourgetteRunnerInfo courgetteRunnerInfo;
    private CourgetteRunResult courgetteRunResult;
    private CourgetteTestStatistics courgetteTestStatistics;

    public CourgetteEventHolder(CourgetteEvent courgetteEvent, CourgetteProperties courgetteProperties, CourgetteRunnerInfo courgetteRunnerInfo, CourgetteRunResult courgetteRunResult) {
        this.courgetteEvent = courgetteEvent;
        this.courgetteProperties = courgetteProperties;
        this.courgetteRunnerInfo = courgetteRunnerInfo;
        this.courgetteRunResult = courgetteRunResult;
    }

    public CourgetteEventHolder(CourgetteEvent courgetteEvent, CourgetteProperties courgetteProperties) {
        this.courgetteEvent = courgetteEvent;
        this.courgetteProperties = courgetteProperties;
    }

    public CourgetteEventHolder(CourgetteEvent courgetteEvent, CourgetteProperties courgetteProperties, CourgetteTestStatistics courgetteTestStatistics) {
        this.courgetteEvent = courgetteEvent;
        this.courgetteProperties = courgetteProperties;
        this.courgetteTestStatistics = courgetteTestStatistics;
    }

    public CourgetteEventHolder(CourgetteEvent courgetteEvent, CourgetteTestStatistics courgetteTestStatistics) {
        this.courgetteEvent = courgetteEvent;
        this.courgetteTestStatistics = courgetteTestStatistics;
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

    public CourgetteTestStatistics getCourgetteTestStatistics() {
        return courgetteTestStatistics;
    }
}
