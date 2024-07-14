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

    public static void printExceptionStackTrace(Exception e) {
        e.printStackTrace();
    }

    public static void printExceptionStackTrace(Throwable e) {
        e.printStackTrace();
    }

    public static void printError(String error) {
        System.err.println(error);
    }
}
