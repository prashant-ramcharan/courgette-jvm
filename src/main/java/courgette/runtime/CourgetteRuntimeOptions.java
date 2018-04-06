package courgette.runtime;

import cucumber.api.CucumberOptions;
import cucumber.runtime.model.CucumberFeature;
import gherkin.ast.Location;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;

public class CourgetteRuntimeOptions {
    private final CourgetteProperties courgetteProperties;
    private final CucumberFeature cucumberFeature;
    private final CucumberOptions cucumberOptions;

    private List<String> runtimeOptions = new ArrayList<>();
    private String rerunFile;
    private String cucumberResourcePath;

    private final int instanceId = UUID.randomUUID().hashCode();

    public CourgetteRuntimeOptions(CourgetteProperties courgetteProperties, CucumberFeature cucumberFeature) {
        this.courgetteProperties = courgetteProperties;
        this.cucumberFeature = cucumberFeature;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();
        this.cucumberResourcePath = cucumberFeature.getUri();

        createRuntimeOptions(cucumberOptions, cucumberResourcePath).forEach((key, value) -> runtimeOptions.addAll(value));
    }

    public CourgetteRuntimeOptions(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();
        this.cucumberFeature = null;

        createRuntimeOptions(cucumberOptions, null).forEach((key, value) -> runtimeOptions.addAll(value));
    }

    public String[] getRuntimeOptions() {
        return copyOf(runtimeOptions.toArray(), runtimeOptions.size(), String[].class);
    }

    public Map<String, List<String>> mapRuntimeOptions() {
        return createRuntimeOptions(cucumberOptions, cucumberResourcePath);
    }

    public String getRerunFile() {
        return rerunFile;
    }

    public String getCucumberRerunFile() {
        final String cucumberRerunFile = cucumberRerunPlugin.apply(courgetteProperties);

        if (cucumberRerunFile == null) {
            return getRerunFile();
        }
        return cucumberRerunFile;
    }

    public List<String> getReportJsFiles() {
        final List<String> reportFiles = new ArrayList<>();

        runtimeOptions.forEach(option -> {
            if (option != null && isReportPlugin.test(option)) {
                String reportFile = option.substring(option.indexOf(":") + 1);

                if (reportFile.endsWith(".html")) {
                    reportFile = reportFile + "/report.js";
                }
                reportFiles.add(reportFile);
            }
        });
        return reportFiles;
    }

    public String getCourgetteReportJson() {
        return "target/courgette-report/data/report.json";
    }

    private Map<String, List<String>> createRuntimeOptions(CucumberOptions cucumberOptions, String path) {
        final Map<String, List<String>> runtimeOptions = new HashMap<>();

        runtimeOptions.put("--glue", optionParser.apply("--glue", envCucumberOptionParser.apply("glue", cucumberOptions.glue())));
        runtimeOptions.put("--tags", optionParser.apply("--tags", envCucumberOptionParser.apply("tags", cucumberOptions.tags())));
        runtimeOptions.put("--plugin", optionParser.apply("--plugin", parsePlugins(envCucumberOptionParser.apply("plugin", cucumberOptions.plugin()))));
        runtimeOptions.put("--format", optionParser.apply("--format", cucumberOptions.format()));
        runtimeOptions.put("--name", optionParser.apply("--name", envCucumberOptionParser.apply("name", cucumberOptions.name())));
        runtimeOptions.put("--junit", optionParser.apply("--junit", envCucumberOptionParser.apply("junit", cucumberOptions.junit())));
        runtimeOptions.put("--snippets", optionParser.apply("--snippets", cucumberOptions.snippets()));
        runtimeOptions.put("--dryRun", Collections.singletonList(cucumberOptions.dryRun() ? "--dry-run" : "--no-dry-run"));
        runtimeOptions.put("--strict", Collections.singletonList(cucumberOptions.strict() ? "--strict" : "--no-strict"));
        runtimeOptions.put("--monochrome", Collections.singletonList(cucumberOptions.monochrome() ? "--monochrome" : "--no-monochrome"));
        runtimeOptions.put(null, featureParser.apply(cucumberOptions.features(), path));
        runtimeOptions.values().removeIf(Objects::isNull);

        return runtimeOptions;
    }

    private BiFunction<String, String[], String[]> envCucumberOptionParser = (systemPropertyName, cucumberOptions) -> {
        String cucumberOption = System.getProperty("cucumber." + systemPropertyName);

        if (cucumberOption != null) {
            final List<String> options = new ArrayList<>();
            Arrays.stream(cucumberOption.split(",")).forEach(t -> options.add(t.trim()));

            String[] cucumberOptionArray = new String[options.size()];
            return options.toArray(cucumberOptionArray);
        }
        return cucumberOptions;
    };

    private String getMultiThreadRerunFile() {
        return getTempDirectory() + courgetteProperties.getSessionId() + "_rerun_" + getFeatureId(cucumberFeature) + ".txt";
    }

    private String getMultiThreadReportFile() {
        return getTempDirectory() + courgetteProperties.getSessionId() + "_thread_report_" + getFeatureId(cucumberFeature);
    }

    private String getFeatureId(CucumberFeature cucumberFeature) {
        final Location location = cucumberFeature.getGherkinFeature().getFeature().getLocation();
        return String.format("%s_%s_%s", location.getLine(), location.getColumn(), instanceId);
    }

    private Function<CourgetteProperties, String> cucumberRerunPlugin = (courgetteProperties) -> {
        final String rerunPlugin = Arrays.stream(courgetteProperties.getCourgetteOptions()
                .cucumberOptions()
                .plugin()).filter(p -> p.startsWith("rerun")).findFirst().orElse(null);

        if (rerunPlugin != null) {
            return rerunPlugin.substring(rerunPlugin.indexOf(":") + 1);
        }
        return null;
    };

    private final Predicate<String> isReportPlugin = (plugin) -> plugin.startsWith("html:") || plugin.startsWith("json:");

    private String[] parsePlugins(String[] plugins) {
        List<String> pluginList = new ArrayList<>();

        if (plugins.length == 0) {
            plugins = new String[]{"json:" + getCourgetteReportJson()};
        }

        asList(plugins).forEach(plugin -> {
            if (isReportPlugin.test(plugin)) {
                if (cucumberFeature != null) {
                    pluginList.add(plugin);

                    String extension = plugin.substring(0, plugin.indexOf(":"));

                    if (!extension.equals("")) {
                        final String reportPath = String.format("%s:%s.%s", extension, getMultiThreadReportFile(), extension);
                        pluginList.add(reportPath);
                    }
                } else {
                    pluginList.add(plugin);
                }
            }
        });

        Predicate<List<String>> alreadyAddedRerunPlugin = (addedPlugins) -> addedPlugins.stream().anyMatch(p -> p.startsWith("rerun:"));

        if (!alreadyAddedRerunPlugin.test(pluginList)) {
            if (cucumberFeature != null) {
                rerunFile = getMultiThreadRerunFile();
            } else {
                final String cucumberRerunFile = cucumberRerunPlugin.apply(courgetteProperties);
                rerunFile = cucumberRerunFile != null ? cucumberRerunFile : "target/courgette-rerun.txt";
            }
            pluginList.add("rerun:" + rerunFile);
        }

        if (pluginList.stream().noneMatch(plugin -> plugin.contains(getCourgetteReportJson()))) {
            pluginList.add("json:" + getCourgetteReportJson());
        }

        return copyOf(pluginList.toArray(), pluginList.size(), String[].class);
    }

    private BiFunction<String, Object, List<String>> optionParser = (name, options) -> {
        final List<String> runOptions = new ArrayList<>();

        final Boolean isStringArray = options instanceof String[];

        if (options == null || (isStringArray && ((String[]) options).length == 0)) {
            return runOptions;
        }

        if (isStringArray) {
            final String[] optionArray = (String[]) options;

            asList(asList(optionArray).toString().split(","))
                    .forEach(value -> {
                        runOptions.add(name);
                        runOptions.add(value.trim().replace("[", "").replace("]", ""));
                    });
        } else {
            if (name != null) {
                runOptions.add(name);
            }
            runOptions.add(options.toString());
        }
        return runOptions;
    };

    private BiFunction<String, String, String> resourceFinder = (resourcePath, featurePath) -> {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String[] resourceFolders = resourcePath.split("/");

        if (resourcePath.equals(featurePath)) {
            return resourcePath;
        }

        Integer startIndex = featurePath.indexOf(resourceFolders[resourceFolders.length - 1]);
        if (startIndex > -1) {
            startIndex = startIndex + (resourceFolders[resourceFolders.length - 1].length() + 1);
        } else {
            startIndex = 0;
        }

        String resourceName = featurePath.substring(startIndex);

        if (resourceFolders.length > 0) {
            final StringBuilder resourcePathBuilder = new StringBuilder();

            for (int i = resourceFolders.length - 1; i >= 0; i--) {
                resourcePathBuilder.insert(0, String.format("%s/", resourceFolders[i]));

                final URL resource = classLoader.getResource(String.format("%s%s", resourcePathBuilder.toString(), resourceName));

                if (resource != null) {
                    return resourcePathBuilder.toString();
                }
            }
        }
        return null;
    };

    private BiFunction<String[], String, List<String>> featureParser = (resourceFeaturePaths, featurePath) -> {
        final List<String> featurePaths = new ArrayList<>();

        if (featurePath != null) {
            for (String resourceFeaturePath : resourceFeaturePaths) {
                final String resource = resourceFinder.apply(resourceFeaturePath, featurePath);

                if (resource != null) {
                    if (featurePath.startsWith(resourceFeaturePath)) {
                        featurePaths.add(featurePath);
                    } else {
                        featurePaths.add(String.format("%s/%s", resourceFeaturePath, featurePath));
                    }
                    return featurePaths;
                }
            }
        }
        featurePaths.addAll(Arrays.asList(resourceFeaturePaths));
        return featurePaths;
    };

    private String getTempDirectory() {
        final String fileSeparator = File.separator;
        final String tmpDir = System.getProperty("java.io.tmpdir");

        if (!tmpDir.endsWith(fileSeparator)) {
            return tmpDir + fileSeparator;
        }
        return tmpDir;
    }
}