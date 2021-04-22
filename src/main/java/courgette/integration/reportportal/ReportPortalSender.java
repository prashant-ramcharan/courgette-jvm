package courgette.integration.reportportal;

import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.event.EventHolder;
import courgette.runtime.event.EventSender;

public class ReportPortalSender implements EventSender {

    private final ReportPortalService reportPortalService;

    public ReportPortalSender(ReportPortalService reportPortalService) {
        this.reportPortalService = reportPortalService;
    }

    @Override
    public synchronized void send(EventHolder eventHolder) {
        final CourgetteRunnerInfo runnerInfo = eventHolder.getCourgetteRunnerInfo();

        switch (eventHolder.getCourgetteEvent()) {
            case TEST_RUN_STARTED:
                reportPortalService.startLaunch();
                reportPortalService.startTestSuite();
                break;
            case TEST_PASSED:
            case TEST_FAILED:
            case TEST_PASSED_AFTER_RERUN:
                reportPortalService.addTest(runnerInfo);
                break;
            case TEST_RUN_FINISHED:
                reportPortalService.finishTestSuite();
                reportPortalService.finishLaunch();
                break;
        }
    }
}
