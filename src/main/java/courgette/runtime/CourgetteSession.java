package courgette.runtime;

import java.util.UUID;

public class CourgetteSession {
    private final String sessionId;

    private static CourgetteSession instance;

    public static CourgetteSession current() {
        if (instance == null) {
            instance = new CourgetteSession(createSessionId());
        }
        return instance;
    }

    private CourgetteSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String sessionId() {
        return sessionId;
    }

    private static String createSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
