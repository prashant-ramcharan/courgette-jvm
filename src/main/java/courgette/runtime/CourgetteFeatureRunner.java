package courgette.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.SystemPropertyUtils.splitAndAddPropertyToList;

public class CourgetteFeatureRunner {
    private Map<String, List<String>> runnerArgs;
    private Boolean output;

    CourgetteFeatureRunner(Map<String, List<String>> runnerArgs, Boolean output) {
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
            printExceptionStackTrace(e);
        }
        return process != null ? process.exitValue() : -1;
    }

    class Builder {

        private static final String CUCUMBER_PROPERTY = "-Dcucumber";
        private static final String CUCUMBER_PROPERTY_PUBLISH_DISABLED = "-Dcucumber.publish.enabled=false";
        private static final String CUCUMBER_PROPERTY_PUBLISH_QUITE = "-Dcucumber.publish.quiet=true";
        private static final String CUCUMBER_PUBLISH_TOKEN = "CUCUMBER_PUBLISH_TOKEN";

        ProcessBuilder buildProcess() {
            final ProcessBuilder builder = new ProcessBuilder();

            environmentVariablesToRemove().forEach(builder.environment()::remove);

            if (output) {
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            builder.redirectErrorStream(true);

            final List<String> commands = new ArrayList<>();
            commands.add("java");
            splitAndAddPropertyToList(CourgetteSystemProperty.VM_OPTIONS, commands);
            commands.addAll(getSystemProperties());
            commands.add("io.cucumber.core.cli.Main");
            runnerArgs.forEach((key, value) -> commands.addAll(value));

            builder.command(commands);
            return builder;
        }

        private List<String> getSystemProperties() {
            final List<String> systemPropertyList = new ArrayList<>();

            System.getProperties().keySet().forEach(property -> systemPropertyList.add(String.format("-D%s=%s", property, System.getProperty(property.toString()))));
            systemPropertyList.removeIf(cucumberSystemPropertiesRequiresRemoval());
            addCucumberSystemProperties(systemPropertyList);
            return systemPropertyList;
        }

        private void addCucumberSystemProperties(final List<String> systemPropertyList) {
            systemPropertyList.add(CUCUMBER_PROPERTY_PUBLISH_DISABLED);
            systemPropertyList.add(CUCUMBER_PROPERTY_PUBLISH_QUITE);
        }

        private Predicate<String> cucumberSystemPropertiesRequiresRemoval() {
            return (s) -> s.startsWith(CUCUMBER_PROPERTY) || s.contains(CUCUMBER_PUBLISH_TOKEN);
        }

        private List<String> environmentVariablesToRemove() {
            final List<String> environment = new ArrayList<>();
            environment.add(CUCUMBER_PUBLISH_TOKEN);
            return environment;
        }
    }
}