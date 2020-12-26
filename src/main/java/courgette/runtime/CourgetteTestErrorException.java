package courgette.runtime;

public class CourgetteTestErrorException extends RuntimeException {

    public CourgetteTestErrorException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public static void throwTestErrorException() {
        throw new CourgetteTestErrorException("There were errors when running the test. Refer to the console for more information.");
    }
}