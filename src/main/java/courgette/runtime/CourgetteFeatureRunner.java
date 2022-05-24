package courgette.runtime;

import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.SystemPropertyUtils.splitAndAddPropertyToList;

public class CourgetteFeatureRunner {
    private final Map<String, List<String>> runnerArgs;
    private final CourgetteProperties courgetteProperties;
    private final CourgettePluginService courgettePluginService;

    CourgetteFeatureRunner(Map<String, List<String>> runnerArgs, CourgetteProperties courgetteProperties, CourgettePluginService courgettePluginService) {
        this.runnerArgs = runnerArgs;
        this.courgetteProperties = courgetteProperties;
        this.courgettePluginService = courgettePluginService;
    }

    public int run() {
        Process process = null;
        Builder thisBuilder = new Builder();
        try {
            final ProcessBuilder builder = thisBuilder.buildProcess();
            process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            printExceptionStackTrace(e);
        } finally {
            if (thisBuilder.getDevice().isPresent()) {
                courgettePluginService.getCourgetteMobileDeviceAllocatorService().deallocateDevice(thisBuilder.getDevice().get());
            }
        }
        return process != null ? process.exitValue() : -1;
    }

    class Builder {
        private static final String CUCUMBER_PROPERTY = "-Dcucumber";
        private static final String CUCUMBER_PROPERTY_PUBLISH_DISABLED = "-Dcucumber.publish.enabled=false";
        private static final String CUCUMBER_PROPERTY_PUBLISH_QUITE = "-Dcucumber.publish.quiet=true";
        private static final String CUCUMBER_PUBLISH_TOKEN = "CUCUMBER_PUBLISH_TOKEN";

        private CourgetteMobileDevice device;

        ProcessBuilder buildProcess() {
            final ProcessBuilder builder = new ProcessBuilder();

            environmentVariablesToRemove().forEach(builder.environment()::remove);

            switch (courgetteProperties.getCourgetteOptions().testOutput()) {
                case CONSOLE:
                    builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    break;
                case FILE:
                    String filePrefix = (runnerArgs.get("retry") != null ? "retry_" : "");
                    builder.redirectOutput(getTestOutputFile(filePrefix));
                    break;
            }

            builder.redirectErrorStream(true);
            final List<String> commands = new ArrayList<>();
            commands.add("java");
            splitAndAddPropertyToList(CourgetteSystemProperty.VM_OPTIONS, commands);
            commands.addAll(getSystemProperties());
            checkCustomClassPath(commands);
            commands.add("io.cucumber.core.cli.Main");
            runnerArgs.forEach((key, value) -> commands.addAll(value));
            builder.command(commands);
            return builder;
        }

        public Optional<CourgetteMobileDevice> getDevice() {
            return Optional.ofNullable(device);
        }

        private List<String> getSystemProperties() {
            final List<String> systemPropertyList = new ArrayList<>();
            System.getProperties().keySet().forEach(property -> systemPropertyList.add(String.format("-D%s=%s", property, System.getProperty(property.toString()))));
            systemPropertyList.removeIf(cucumberSystemPropertiesRequiresRemoval());
            addCucumberSystemProperties(systemPropertyList);
            addCourgetteMobileDeviceAllocatorProperties(systemPropertyList);
            return systemPropertyList;
        }

        private void addCucumberSystemProperties(final List<String> systemPropertyList) {
            systemPropertyList.add(CUCUMBER_PROPERTY_PUBLISH_DISABLED);
            systemPropertyList.add(CUCUMBER_PROPERTY_PUBLISH_QUITE);
        }

        private void addCourgetteMobileDeviceAllocatorProperties(final List<String> systemPropertyList) {
            if (courgetteProperties.isMobileDeviceAllocationPluginEnabled()) {
                device = courgettePluginService.getCourgetteMobileDeviceAllocatorService().allocateDevice();
                systemPropertyList.add(String.format("-D%s=%s", CourgetteSystemProperty.DEVICE_NAME_SYSTEM_PROPERTY, device.getDeviceName()));
                systemPropertyList.add(String.format("-D%s=%s", CourgetteSystemProperty.PARALLEL_PORT_SYSTEM_PROPERTY, device.getParallelPort()));
                if (device.getUdid() != null) {
                    systemPropertyList.add(String.format("-D%s=%s", CourgetteSystemProperty.UDID_SYSTEM_PROPERTY, device.getUdid()));
                }
            }
        }

        private Predicate<String> cucumberSystemPropertiesRequiresRemoval() {
            return (s) -> s.startsWith(CUCUMBER_PROPERTY) || s.contains(CUCUMBER_PUBLISH_TOKEN);
        }

        private List<String> environmentVariablesToRemove() {
            final List<String> environment = new ArrayList<>();
            environment.add(CUCUMBER_PUBLISH_TOKEN);
            return environment;
        }

        private void checkCustomClassPath(List<String> commands) {
            if (courgetteProperties.useCustomClasspath()) {
                commands.removeIf(c -> c.startsWith("-Djava.class.path"));
                commands.add("-cp");
                commands.add(String.join(File.pathSeparator, courgetteProperties.getCourgetteOptions().classPath()));
            }
        }

        private File getTestOutputFile(String prefix) {
            final File testOutputFile = new File(testOutputDirectory() + testOutputFilename(prefix));
            try {
                Files.createFile(testOutputFile.toPath());
                return testOutputFile;
            } catch (IOException e) {
                printExceptionStackTrace(e);
            }
            return new File(FileUtils.formatFilePath(courgetteProperties.getCourgetteOptions().reportTargetDir()) + testOutputFilename(prefix));
        }

        private String testOutputDirectory() {
            final String target = courgetteProperties.getCourgetteOptions().reportTargetDir();
            final File testOutputDirectory = new File(FileUtils.formatFilePath(target) + "courgette-test-output");
            if (!testOutputDirectory.exists()) {
                testOutputDirectory.mkdir();
            }
            return FileUtils.formatFilePath(testOutputDirectory.getPath());
        }

        private String testOutputFilename(String prefix) {
            return prefix + Arrays.stream(runnerArgs.get(null).get(0)
                            .split(File.separator))
                    .reduce((x, y) -> y).get()
                    .replace(".feature", "") + Instant.now().toEpochMilli() + ".log";
        }
    }
}