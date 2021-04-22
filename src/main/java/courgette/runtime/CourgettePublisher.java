package courgette.runtime;

import courgette.runtime.event.CourgetteEvent;

public interface CourgettePublisher {

    void publish(CourgetteEvent event);

    void publish(CourgetteEvent event, CourgetteRunResult result);

    void publish(CourgetteEvent event, CourgetteRunnerInfo runnerInfo, CourgetteRunResult result);
}
