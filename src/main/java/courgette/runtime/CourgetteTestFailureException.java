package courgette.runtime;

public class CourgetteTestFailureException extends RuntimeException {

    public CourgetteTestFailureException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}