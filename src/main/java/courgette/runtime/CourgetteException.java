package courgette.runtime;

public class CourgetteException extends RuntimeException {

    public CourgetteException(String message) {
        super(message);
    }

    public CourgetteException(String message, Throwable e) {
        super(message, e);
    }

    public CourgetteException(Throwable e) {
        super(e);
    }
}
