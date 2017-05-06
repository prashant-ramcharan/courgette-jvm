package courgette.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CourgetteFeatureRunner {
    private Map<String, List<String>> runnerArgs;
    private Boolean output;
    private static String classpath;
    private static List<String> systemProperties;

    public CourgetteFeatureRunner(Map<String, List<String>> runnerArgs, Boolean output) {
        this.runnerArgs = runnerArgs;
        this.output = output;
    }

    public int run() {
        Process process = null;
        try {
            final ProcessBuilder builder = new ProcessBuilder();

            if (output) {
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            builder.redirectErrorStream(Boolean.TRUE);

            final List<String> commands = new ArrayList<>();
            commands.add("java");
            commands.add("-cp");
            commands.add("\"" + classpath + "\"");
            systemProperties.forEach(commands::add);
            commands.add("cucumber.api.cli.Main");
            this.runnerArgs.entrySet().forEach(entry -> entry.getValue().forEach(commands::add));

            builder.command(commands);
            process = builder.start();
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

        classpath = classPathBuilder.toString();

        final List<String> systemPropertyList = new ArrayList<>();
        System.getProperties().keySet().forEach(property -> systemPropertyList.add(String.format("-D%s=%s", property, System.getProperty(property.toString()))));

        systemProperties = systemPropertyList;
    }
}