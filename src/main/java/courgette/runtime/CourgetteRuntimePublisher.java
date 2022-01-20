package courgette.runtime;

import courgette.runtime.event.CourgetteEventHolder;

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
    public void publish(CourgetteEventHolder eventHolder) {
        publishers.forEach(p -> p.publish(eventHolder));
    }
}
