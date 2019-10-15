package courgette.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static courgette.runtime.utils.SystemPropertyUtils.splitAndAddPropertyToList;

public class CourgetteFeatureRunner {
    private Map<String, List<String>> runnerArgs;
    private Boolean output;

    public CourgetteFeatureRunner(Map<String, List<String>> runnerArgs, Boolean output) {
        this.runnerArgs = runnerArgs;
        this.output = output;
    }

    public int run() {
        Process process = null;
        try {
            final ProcessBuilder builder = new Builder().buildProcess();
            process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return process != null ? process.exitValue() : -1;
    }

    class Builder {

        ProcessBuilder buildProcess() {
            final ProcessBuilder builder = new ProcessBuilder();

            if (output) {
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            builder.redirectErrorStream(true);

            final List<String> commands = new ArrayList<>();
            commands.add("java");
            splitAndAddPropertyToList(CourgetteSystemProperty.VM_OPTIONS, commands);

            if (isJava8()) {
                commands.add("-cp");
                commands.add("\"" + getClassPath() + "\"");
            } else {
                commands.add("-p");
                commands.add("jrt");
            }

            commands.addAll(getSystemProperties());

            commands.add("io.cucumber.core.cli.Main");
            runnerArgs.forEach((key, value) -> commands.addAll(value));

            builder.command(commands);
            return builder;
        }

        private boolean isJava8() {
            return System.getProperty("java.version").startsWith("1.8");
        }

        private String getClassPath() {
            final StringBuffer classPathBuilder = new StringBuffer();

            final URL[] classPathUrls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
            Arrays.asList(classPathUrls).forEach(url -> classPathBuilder.append(String.format("%s%s", url.getPath().replace("file:", ""), File.pathSeparator)));

            return classPathBuilder.toString();
        }

        private List<String> getSystemProperties() {
            final List<String> systemPropertyList = new ArrayList<>();

            System.getProperties().keySet().forEach(property -> systemPropertyList.add(String.format("-D%s=%s", property, System.getProperty(property.toString()))));
            systemPropertyList.removeIf(systemProperty -> systemProperty.startsWith("-Dcucumber"));

            return systemPropertyList;
        }
    }
}