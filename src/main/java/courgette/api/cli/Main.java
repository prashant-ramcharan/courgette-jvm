package courgette.api.cli;

public class Main {

    public static void main(String[] argv) throws Throwable {
        cucumber.api.cli.Main.main(argv);
    }

    public static byte run(String[] argv) throws Throwable {
        return cucumber.api.cli.Main.run(argv, Thread.currentThread().getContextClassLoader());
    }
}