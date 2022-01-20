package courgette.runtime;

import courgette.runtime.event.CourgetteEventHolder;

public interface CourgettePublisher {

    void publish(CourgetteEventHolder eventHolder);
}
