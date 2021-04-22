package courgette.runtime;

import courgette.runtime.event.CourgetteEvent;

import java.util.HashSet;
import java.util.Set;

public class CourgetteRuntimePublisher implements CourgettePublisher {

    private final Set<CourgettePublisher> publishers = new HashSet<>();

    public CourgetteRuntimePublisher(Set<CourgettePublisher> publishers) {
        if (!publishers.isEmpty()) {
            this.publishers.addAll(publishers);
        }
    }

    @Override
    public void publish(CourgetteEvent event) {
        publishers.forEach(p -> p.publish(event));
    }

    @Override
    public void publish(CourgetteEvent event, CourgetteRunResult result) {
        publishers.forEach(p -> p.publish(event, result));
    }

    @Override
    public void publish(CourgetteEvent event, CourgetteRunnerInfo runnerInfo, CourgetteRunResult result) {
        publishers.forEach(p -> p.publish(event, runnerInfo, result));
    }
}
