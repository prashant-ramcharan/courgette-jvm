package courgette.runtime;

import courgette.api.CucumberOptions;
import courgette.runtime.utils.SystemPropertyUtils;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;

class CourgetteRuntimeOptions {
    private final CourgetteProperties courgetteProperties;
    private final CourgetteReportOptions courgetteReportOptions;
    private final Feature feature;
    private final CucumberOptions cucumberOptions;
    private final String reportTargetDir;
    private final List<String> runtimeOptions = new ArrayList<>();
    private String cucumberResourcePath;

    private final int instanceId = Math.abs(UUID.randomUUID().hashCode());

    CourgetteRuntimeOptions(CourgetteProperties courgetteProperties,
                            CourgetteReportOptions courgetteReportOptions,
                            Feature feature) {
        this.courgetteProperties = courgetteProperties;
        this.courgetteReportOptions = courgetteReportOptions;
        this.feature = feature;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();
        this.cucumberResourcePath = determineResourcePath(feature);
        this.reportTargetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();

        createRuntimeOptions(cucumberOptions, cucumberResourcePath).forEach((key, value) -> runtimeOptions.addAll(value));
    }

    CourgetteRuntimeOptions(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        this.courgetteReportOptions = null;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();
        this.feature = null;
        this.reportTargetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();

        createRuntimeOptions(cucumberOptions, null).forEach((key, value) -> runtimeOptions.addAll(value));
    }

    public RuntimeOptions getRuntimeOptions() {
        return new CommandlineOptionsParser(System.out).parse(runtimeOptions.toArray(new String[]{})).build();
    }

    public Map<String, List<String>> mapRuntimeOptions() {
        return createRuntimeOptions(cucumberOptions, cucumberResourcePath);
    }

    public String getRerunFile() {
        return courgetteReportOptions.getRerunFile().orElse(null);
    }

    public String getCucumberRerunFile() {
        return cucumberRerunPlugin.apply(courgetteProperties);
    }

    public List<String> getReportFiles() {
        final List<String> reportFiles = new ArrayList<>();

        runtimeOptions.forEach(option -> {
            if (option != null && isReportPlugin.test(option)) {
                String reportFile = option.substring(option.indexOf(":") + 1);
                reportFiles.add(reportFile);
            }
        });
        return reportFiles;
    }

    public String getSessionReportsDirectory() {
        return String.format("%s/session-reports/%s/", reportTargetDir, courgetteProperties.getSessionId());
    }

    public String getCourgetteRunLogJson() {
        return String.format("%s/courgette-run-%s.json", reportTargetDir, courgetteProperties.getSessionId());
    }

    private Map<String, List<String>> createRuntimeOptions(CucumberOptions cucumberOptions, String path) {
        final Map<String, List<String>> runtimeOptions = new HashMap<>();

        runtimeOptions.put("--glue", optionParser.apply("--glue", envCucumberOptionParser.apply("glue", cucumberOptions.glue())));
        runtimeOptions.put("--extraGlue", optionParser.apply("--glue", envCucumberOptionParser.apply("extraGlue", cucumberOptions.extraGlue())));
        runtimeOptions.put("--filter.tags", optionParser.apply("--tags", envCucumberOptionParser.apply("tags", cucumberOptions.tags())));
        runtimeOptions.put("--plugin", optionParser.apply("--plugin", parsePlugins(envCucumberOptionParser.apply("plugin", cucumberOptions.plugin()))));
        runtimeOptions.put("--name", optionParser.apply("--name", envCucumberOptionParser.apply("name", cucumberOptions.name())));
        runtimeOptions.put("--snippets", optionParser.apply("--snippets", cucumberOptions.snippets().name().toLowerCase()));
        runtimeOptions.put("--dryRun", Collections.singletonList(cucumberOptions.dryRun() ? "--dry-run" : "--no-dry-run"));
        runtimeOptions.put("--monochrome", Collections.singletonList(cucumberOptions.monochrome() ? "--monochrome" : "--no-monochrome"));
        runtimeOptions.put(null, featureParser.apply(envCucumberOptionParser.apply("features", cucumberOptions.features()), path));

        if (!cucumberOptions.objectFactory().getName().equals("courgette.runtime.CourgetteNoObjectFactory")) {
            runtimeOptions.put("--object-factory", optionParser.apply("--object-factory", cucumberOptions.objectFactory().getName()));
        }

        runtimeOptions.values().removeIf(Objects::isNull);

        return runtimeOptions;
    }

    private final BiFunction<String, String[], String[]> envCucumberOptionParser = (propertyName, cucumberOptions) -> {
        String cucumberOption = SystemPropertyUtils.fromSystemEnvOrProperty("cucumber." + propertyName);

        if (cucumberOption != null && !cucumberOption.trim().isEmpty()) {
            final List<String> options = new ArrayList<>();
            Arrays.stream(cucumberOption.split(",")).forEach(t -> options.add(t.trim()));

            String[] cucumberOptionArray = new String[options.size()];
            return options.toArray(cucumberOptionArray);
        }
        return cucumberOptions;
    };

    private String getSessionFeatureReportFile() {
        final String featureName = Arrays.stream(feature.getUri().getPath().split("/")).reduce((x, y) -> y)
                .get().replace(".feature", "")
                .toLowerCase();
        return getSessionReportsDirectory() + String.format("%s_%s", featureName, instanceId);
    }

    private final Function<CourgetteProperties, String> cucumberRerunPlugin = (courgetteProperties) -> {
        final String rerunPlugin = Arrays.stream(courgetteProperties.getCourgetteOptions()
                .cucumberOptions()
                .plugin()).filter(p -> p.startsWith("rerun")).findFirst().orElse(null);

        if (rerunPlugin != null) {
            return rerunPlugin.substring(rerunPlugin.indexOf(":") + 1);
        }
        return null;
    };

    private final Predicate<String> isReportPlugin = (plugin) -> plugin.startsWith("html:") || plugin.startsWith("json:") || plugin.startsWith("junit:") || plugin.startsWith("message:");

    private String[] parsePlugins(String[] plugins) {
        HashSet<String> pluginCollection = new HashSet<>(asList(plugins));
        removeReportPlugins(pluginCollection);
        addRuntimeReportPlugins(pluginCollection);
        addRuntimeRerunPlugin(pluginCollection);
        return copyOf(pluginCollection.toArray(), pluginCollection.size(), String[].class);
    }

    private void addRuntimeReportPlugins(HashSet<String> plugins) {
        if (feature != null) {
            plugins.add("json:" + courgetteReportOptions.getJsonFile());
            plugins.add("message:" + courgetteReportOptions.getNdJsonFile());

            if (courgetteProperties.shouldPersistCucumberJsonReports()) {
                String sessionFeatureReportFile = getSessionFeatureReportFile();
                plugins.add(String.format("json:%s.json", sessionFeatureReportFile));
                plugins.add(String.format("message:%s.ndjson", sessionFeatureReportFile));
            }

            courgetteReportOptions.getXmlFile().ifPresent(plugin -> plugins.add("junit:" + plugin));
        }
    }

    private void addRuntimeRerunPlugin(HashSet<String> plugins) {
        if (feature != null) {
            courgetteReportOptions.getRerunFile().ifPresent(plugin -> plugins.add("rerun:" + plugin));
        }
    }

    private void removeReportPlugins(HashSet<String> plugins) {
        if (feature != null) {
            plugins.removeIf(plugin -> plugin.startsWith("html") || plugin.startsWith("json") || plugin.startsWith("message") || plugin.startsWith("junit"));
        }
    }

    private final BiFunction<String, Object, List<String>> optionParser = (name, options) -> {
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

    private final BiFunction<String[], String, List<String>> featureParser = (resourceFeaturePaths, featurePath) -> {
        final List<String> featurePaths = new ArrayList<>();
        if (featurePath == null) {
            featurePaths.addAll(Arrays.asList(resourceFeaturePaths));
        } else {
            featurePaths.add(featurePath);
        }
        return featurePaths;
    };

    private String determineResourcePath(Feature feature) {
        return String.format("%s:%s",
                feature.getUri().getScheme(),
                feature.getUri().getSchemeSpecificPart().replace("//", "/"));
    }
}