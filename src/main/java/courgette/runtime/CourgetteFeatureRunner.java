package courgette.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CourgetteFeatureRunner {
    private Map<String, List<String>> runnerArgs;
    private static String classpath;

    public CourgetteFeatureRunner(Map<String, List<String>> runnerArgs) {
        this.runnerArgs = runnerArgs;
    }

    public int run() {
        final StringBuffer cliArgs = new StringBuffer();
        this.runnerArgs.entrySet().forEach(entry -> entry.getValue().forEach(e -> cliArgs.append(String.format(" %s", e))));

        final String processArgs = String.format("java -cp %s cucumber.api.cli.Main %s", classpath, cliArgs);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(processArgs);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return process != null ? process.exitValue() : -1;
    }

    static {
        final StringBuffer classPathBuilder = new StringBuffer();

        final URL[] classPathUrls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
        Arrays.asList(classPathUrls).forEach(url -> classPathBuilder.append(String.format("%s%s", url.getPath().replace("file:", ""), File.pathSeparator)));

        classPathBuilder.insert(0, "\"");
        classPathBuilder.insert(classPathBuilder.length(), "\"");

        classpath = classPathBuilder.toString();
    }
}