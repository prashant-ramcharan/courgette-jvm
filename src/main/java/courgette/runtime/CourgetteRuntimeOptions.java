package courgette.runtime;

import cucumber.api.CucumberOptions;
import cucumber.runtime.model.CucumberFeature;

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

    private final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private final String DEFAULT_RERUN_PLUGIN = "target/courgette-rerun.txt";

    public CourgetteRuntimeOptions(CourgetteProperties courgetteProperties, CucumberFeature cucumberFeature) {
        this.courgetteProperties = courgetteProperties;
        this.cucumberFeature = cucumberFeature;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();

        createRuntimeOptions(cucumberOptions, cucumberFeature).entrySet().forEach(entry -> runtimeOptions.addAll(entry.getValue()));
    }

    public CourgetteRuntimeOptions(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        this.cucumberFeature = null;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();

        createRuntimeOptions(cucumberOptions, null).entrySet().forEach(entry -> runtimeOptions.addAll(entry.getValue()));
    }

    public String[] getRuntimeOptions() {
        return copyOf(runtimeOptions.toArray(), runtimeOptions.size(), String[].class);
    }

    public Map<String, List<String>> getRuntimeOptionsMap() {
        return createRuntimeOptions(cucumberOptions, cucumberFeature);
    }

    public Map<String, List<String>> getRerunRuntimeOptionsMap(String rerunFeatureScenario) {
        Map<String, List<String>> runtimeOptionMap = createRuntimeOptions(cucumberOptions, cucumberFeature);

        List<String> plugins = runtimeOptionMap.getOrDefault("--plugin", new ArrayList<>());

        final int rerunPluginIndex = plugins.indexOf(plugins.stream().filter(p -> p.startsWith("rerun")).findFirst().orElse(null));
        if (rerunPluginIndex > 0) {
            plugins.remove(rerunPluginIndex);
            plugins.remove(rerunPluginIndex - 1);
        }

        runtimeOptionMap.put("--plugin", plugins);

        runtimeOptionMap.remove("--tags");
        runtimeOptionMap.put(null, new ArrayList<String>() {
            {
                add(rerunFeatureScenario);
            }
        });

        return runtimeOptionMap;
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

    private Map<String, List<String>> createRuntimeOptions(CucumberOptions cucumberOptions, CucumberFeature feature) {
        final Map<String, List<String>> runtimeOptions = new HashMap<>();

        runtimeOptions.put("--glue", optionParser.apply("--glue", cucumberOptions.glue()));
        runtimeOptions.put("--tags", optionParser.apply("--tags", cucumberOptions.tags()));
        runtimeOptions.put("--plugin", optionParser.apply("--plugin", parsePlugins(cucumberOptions.plugin())));
        runtimeOptions.put("--format", optionParser.apply("--format", parsePlugins(cucumberOptions.format())));
        runtimeOptions.put("--name", optionParser.apply("--name", cucumberOptions.name()));
        runtimeOptions.put("--junit", optionParser.apply("--junit", cucumberOptions.junit()));
        runtimeOptions.put("--snippets", optionParser.apply("--snippets", cucumberOptions.snippets()));
        runtimeOptions.put(null, featureParser.apply(cucumberOptions.features(), feature == null ? null : feature.getPath()));
        runtimeOptions.values().removeIf(Objects::isNull);

        return runtimeOptions;
    }

    private String getMultiThreadRerunFile() {
        return TMP_DIR + courgetteProperties.getSessionId() + "_rerun_" + cucumberFeature.getGherkinFeature().getId() + ".txt";
    }

    private String getMultiThreadReportFile() {
        return TMP_DIR + courgetteProperties.getSessionId() + "_thread_report_" + cucumberFeature.getGherkinFeature().getId();
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

        if (plugins.length > 0) {
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
                    rerunFile = cucumberRerunFile != null ? cucumberRerunFile : DEFAULT_RERUN_PLUGIN;
                }
                pluginList.add("rerun:" + rerunFile);
            }
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

        if (resourceFolders.length > 0) {
            final StringBuilder resourcePathBuilder = new StringBuilder();

            for (int i = resourceFolders.length - 1; i >= 0; i--) {
                resourcePathBuilder.insert(0, String.format("%s/", resourceFolders[i]));

                final URL resource = classLoader.getResource(String.format("%s%s", resourcePathBuilder.toString(), featurePath));

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
                final String foundResource = resourceFinder.apply(resourceFeaturePath, featurePath);

                if (foundResource != null) {
                    featurePaths.add(String.format("%s/%s", resourceFeaturePath, featurePath));
                    return featurePaths;
                }
            }
        }
        Arrays.asList(resourceFeaturePaths).forEach(featurePaths::add);
        return featurePaths;
    };
}