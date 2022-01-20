package courgette.api;

import courgette.runtime.CourgetteException;
import courgette.runtime.CourgetteRunOptions;
import courgette.runtime.CourgetteSession;
import courgette.runtime.CourgetteTestStatistics;

import java.util.Arrays;

public final class CourgetteRunInfo {
    static String sessionId;
    static Class<?> courgetteRunnerClass;
    static CourgetteTestStatistics testStatistics;

    static {
        courgetteRunnerClass = findCourgetteRunnerClass();
        if (courgetteRunnerClass == null) {
            throw new CourgetteException("CourgetteRunInfo can only be used in a Courgette runner class.");
        }
        sessionId = CourgetteSession.current().sessionId();
        testStatistics = CourgetteTestStatistics.current();
    }

    public static String sessionId() {
        return sessionId;
    }

    public static CourgetteOptions courgetteOptions() {
        return new CourgetteRunOptions(courgetteRunnerClass);
    }

    public static CourgetteTestStatistics testStatistics() {
        return testStatistics;
    }

    private static Class<?> findCourgetteRunnerClass() {
        StackTraceElement runnerStackTraceElement = findRunnerStackTraceElement();

        if (runnerStackTraceElement == null) {
            return null;
        }

        String runnerClassName = runnerStackTraceElement.getClassName();
        try {
            Class<?> runnerClass = Class.forName(runnerClassName);
            if (Arrays.stream(runnerClass.getDeclaredAnnotations())
                    .anyMatch(annotation -> annotation.annotationType().equals(CourgetteOptions.class))) {
                return runnerClass;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private static StackTraceElement findRunnerStackTraceElement() {
        int index = 0;
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (ste.getClassName().equals(CourgetteRunInfo.class.getName()) && ste.getMethodName().equals("<clinit>")) {
                return Thread.currentThread().getStackTrace()[index + 1];
            } else {
                index++;
            }
        }
        return null;
    }
}
