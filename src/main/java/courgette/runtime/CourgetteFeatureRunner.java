package courgette.runtime;

import cucumber.runtime.model.CucumberFeature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static courgette.runtime.utils.SystemPropertyUtils.splitAndAddPropertyToList;

public class CourgetteFeatureRunner {
    private CucumberFeature cucumberFeature;
    private Map<String, List<String>> runnerArgs;
    private Boolean output;
    private static String classpath;
    private static List<String> systemProperties;

    public CourgetteFeatureRunner(CucumberFeature cucumberFeature, Map<String, List<String>> runnerArgs, Boolean output) {
        this.cucumberFeature = cucumberFeature;
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

            String featureLang = this.parseFeatureLanguage(cucumberFeature);
            final String featureName = cucumberFeature.getGherkinFeature().getName();
            final List<String> commands = new ArrayList<>();
            commands.add("java");
            splitAndAddPropertyToList(CourgetteSystemProperty.VM_OPTIONS, commands);
            commands.add("-DfeatureLang=" + featureLang);
            commands.add("-DfeatureName=" + featureName);
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

    // This method created because cucumberFeature.getI18n() isn't thread-safe
    // and always returns the last feature iso code
    private String parseFeatureLanguage(CucumberFeature cucumberFeature) {
        String regex = "#\\s*language\\s*:\\s*([^\\s]+).*";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        return cucumberFeature.getGherkinFeature().getComments().stream().filter(c -> {
                    String comment = c.getValue().trim();
                    return pattern.matcher(comment).matches();
                }
        ).map(c -> {
            String comment = c.getValue().trim();
            Matcher matcher = pattern.matcher(comment);
            if (matcher.matches()) {
                return matcher.group(1);
            } else return "en";
        }).findFirst().orElse("en");
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