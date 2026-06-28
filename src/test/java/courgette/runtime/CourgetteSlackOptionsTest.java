package courgette.runtime;

import courgette.runtime.event.CourgetteEvent;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourgetteSlackOptionsTest {
    @Test
    public void validatesCompleteSlackConfiguration() {
        CourgetteSlackOptions options = new CourgetteSlackOptions(
                "https://hooks.slack.test",
                Arrays.asList("#builds", "#qa"),
                "test-id",
                Collections.singletonList(CourgetteEvent.TEST_RUN_SUMMARY));

        assertTrue(options.shouldValidate());
        assertTrue(options.isValid());
    }

    @Test
    public void invalidWhenWebhookOrChannelsAreMissing() {
        assertFalse(new CourgetteSlackOptions("", Collections.emptyList(), "", Collections.emptyList()).shouldValidate());
        assertFalse(new CourgetteSlackOptions("", Collections.singletonList("#qa"), "", Collections.emptyList()).isValid());
        assertFalse(new CourgetteSlackOptions("https://hooks.slack.test", Collections.singletonList(" "), "", Collections.emptyList()).isValid());
    }
}
