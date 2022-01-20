package courgette.integration.reportportal;

import courgette.runtime.event.CourgetteEventHolder;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgettePublisher;
import courgette.runtime.event.EventPublisher;
import courgette.runtime.event.EventSubscriberCreator;
import io.cucumber.core.gherkin.Feature;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static courgette.runtime.event.CourgetteEvent.TEST_FAILED;
import static courgette.runtime.event.CourgetteEvent.TEST_PASSED;
import static courgette.runtime.event.CourgetteEvent.TEST_PASSED_AFTER_RERUN;
import static courgette.runtime.event.CourgetteEvent.TEST_RUN_FINISHED;
import static courgette.runtime.event.CourgetteEvent.TEST_RUN_STARTED;

public class ReportPortalPublisher implements CourgettePublisher {

    private Optional<EventPublisher> eventPublisher = Optional.empty();

    public ReportPortalPublisher(CourgetteProperties courgetteProperties, List<Feature> features) {

        if (courgetteProperties.isReportPortalPluginEnabled()) {
            final ReportPortalService reportPortalService = new ReportPortalService(ReportPortalProperties.getInstance(), courgetteProperties, features);

            eventPublisher = Optional.of(new EventPublisher(new ReportPortalSender(reportPortalService)));

            EventSubscriberCreator.createEventSubscribers(eventPublisher.get(),
                    Arrays.asList(TEST_RUN_STARTED, TEST_RUN_FINISHED, TEST_PASSED, TEST_PASSED_AFTER_RERUN, TEST_FAILED));
        }
    }

    @Override
    public void publish(CourgetteEventHolder eventHolder) {
        eventPublisher.ifPresent(p -> p.publishEvent(eventHolder));
    }
}
