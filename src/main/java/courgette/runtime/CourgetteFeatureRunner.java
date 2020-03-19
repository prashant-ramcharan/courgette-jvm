package courgette.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static courgette.runtime.utils.SystemPropertyUtils.splitAndAddPropertyToList;

public class CourgetteFeatureRunner {
    private Map<String, List<String>> runnerArgs;
    private Boolean output;
    private Path customClassPath;

    public CourgetteFeatureRunner(Map<String, List<String>> runnerArgs, Boolean output, Path customClassPath) {
        this.runnerArgs = runnerArgs;
        this.output = output;
        this.customClassPath = customClassPath;
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
                final boolean useCustomClassPath = customClassPath != null;
                commands.add("-cp");
                commands.add("\"" + getClassPath(useCustomClassPath) + "\"");
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

        private String getClassPath(boolean useCustomClassPath) {
            final StringBuffer classPathBuilder = new StringBuffer();

            if (useCustomClassPath) {
                getCustomClassPathFiles().forEach(file -> classPathBuilder.append(String.format("%s%s", file.getPath(), File.pathSeparator)));
            } else {
                final URL[] classPathUrls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
                Arrays.asList(classPathUrls).forEach(url -> classPathBuilder.append(String.format("%s%s", url.getPath().replace("file:", ""), File.pathSeparator)));
            }
            return classPathBuilder.toString();
        }

        private List<File> getCustomClassPathFiles() {
            List<File> classPathFiles = new ArrayList<>();
            if (customClassPath != null) {
                File classPathFile = new File(customClassPath.toFile().toURI());
                File[] files = classPathFile.listFiles();
                if (files != null) {
                    classPathFiles.addAll(Arrays.asList(files));
                } else {
                    throw new CourgetteException("Unable to use a custom classpath because the required classpath files are missing.");
                }
            }
            return classPathFiles;
        }

        private List<String> getSystemProperties() {
            final List<String> systemPropertyList = new ArrayList<>();

            System.getProperties().keySet().forEach(property -> systemPropertyList.add(String.format("-D%s=%s", property, System.getProperty(property.toString()))));
            systemPropertyList.removeIf(systemProperty -> systemProperty.startsWith("-Dcucumber"));

            return systemPropertyList;
        }
    }
}