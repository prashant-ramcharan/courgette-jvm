package courgette.runtime;

import courgette.api.CucumberOptions;
import courgette.integration.reportportal.ReportPortalProperties;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;

import java.io.File;
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
    private final Feature feature;
    private final CucumberOptions cucumberOptions;
    private final String reportTargetDir;
    private final List<String> runtimeOptions = new ArrayList<>();

    private String rerunFile;
    private String cucumberResourcePath;

    private final int instanceId = Math.abs(UUID.randomUUID().hashCode());

    CourgetteRuntimeOptions(CourgetteProperties courgetteProperties, Feature feature) {
        this.courgetteProperties = courgetteProperties;
        this.feature = feature;
        this.cucumberOptions = courgetteProperties.getCourgetteOptions().cucumberOptions();
        this.cucumberResourcePath = feature.getUri().getSchemeSpecificPart();
        this.reportTargetDir = courgetteProperties.getCourgetteOptions().reportTargetDir();

        createRuntimeOptions(cucumberOptions, cucumberResourcePath).forEach((key, value) -> runtimeOptions.addAll(value));
    }

    CourgetteRuntimeOptions(CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
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
        return rerunFile;
    }

    public String getCucumberRerunFile() {
        final String cucumberRerunFile = cucumberRerunPlugin.apply(courgetteProperties);

        if (cucumberRerunFile == null) {
            return getRerunFile();
        }
        return cucumberRerunFile;
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

    public String getJsonReportFile() {
        return getReportFiles().stream()
                .filter(isJsonReportPlugin)
                .filter(r -> r.contains(courgetteProperties.getSessionId()))
                .findFirst()
                .orElse(null);
    }

    public String getCourgetteReportDataDirectory() {
        return reportTargetDir + "/courgette-report/data";
    }

    public String getSessionReportsDirectory() {
        return String.format("%s/session-reports/%s/", reportTargetDir, courgetteProperties.getSessionId());
    }

    public String getCourgetteReportJson() {
        return String.format("%s/report.json", getCourgetteReportDataDirectory());
    }

    public String getCourgetteReportNdJson() {
        return String.format("%s/report.ndjson", getCourgetteReportDataDirectory());
    }

    public String getCourgetteReportXmlForReportPortal() {
        final ReportPortalProperties reportPortalProperties = ReportPortalProperties.getInstance();
        return String.format("%s/%s.xml", getCourgetteReportDataDirectory(), reportPortalProperties.getLaunchName());
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

    private final BiFunction<String, String[], String[]> envCucumberOptionParser = (systemPropertyName, cucumberOptions) -> {
        String cucumberOption = System.getProperty("cucumber." + systemPropertyName);

        if (cucumberOption != null && cucumberOption.trim().length() > 0) {
            final List<String> options = new ArrayList<>();
            Arrays.stream(cucumberOption.split(",")).forEach(t -> options.add(t.trim()));

            String[] cucumberOptionArray = new String[options.size()];
            return options.toArray(cucumberOptionArray);
        }
        return cucumberOptions;
    };

    private String getMultiThreadRerunFile() {
        return getTempDirectory() + courgetteProperties.getSessionId() + "_rerun_" + getFeatureId(feature) + ".txt";
    }

    private String getMultiThreadReportFile() {
        return getTempDirectory() + courgetteProperties.getSessionId() + "_thread_report_" + getFeatureId(feature);
    }

    private String getFeatureReportFile() {
        final String featureName = Arrays.stream(feature.getUri().getPath().split("/")).reduce((x, y) -> y)
                .get().replace(".feature", "")
                .toLowerCase();
        return getSessionReportsDirectory() + String.format("%s_%s", featureName, instanceId);
    }

    private String getFeatureId(Feature feature) {
        return String.format("%s_%s", feature.hashCode(), instanceId);
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

    private final Predicate<String> isJsonReportPlugin = (plugin) -> plugin.endsWith(".json");

    private String[] parsePlugins(String[] plugins) {
        HashSet<String> pluginCollection = new HashSet<>();

        plugins = addDefaultPlugins(plugins);

        asList(plugins).forEach(plugin -> {
            if (isReportPlugin.test(plugin)) {
                if (feature != null) {
                    pluginCollection.add(plugin);

                    String extension = plugin.substring(0, plugin.indexOf(":"));

                    if (extension.equalsIgnoreCase("junit")) {
                        pluginCollection.remove(plugin);
                        final String reportPath = String.format("junit:%s.xml", getMultiThreadReportFile());
                        pluginCollection.add(reportPath);
                    } else if (extension.equalsIgnoreCase("message")) {
                        final String reportPath = String.format("message:%s.ndjson", getMultiThreadReportFile());
                        pluginCollection.add(reportPath);
                    } else {
                        if (!extension.equals("") && !extension.equals("html")) {
                            final String reportPath = String.format("%s:%s.%s", extension, getMultiThreadReportFile(), extension);
                            pluginCollection.add(reportPath);
                        }
                    }
                } else {
                    pluginCollection.add(plugin);
                }
            } else {
                pluginCollection.add(plugin);
            }
        });

        Predicate<HashSet<String>> alreadyAddedRerunPlugin = (addedPlugins) -> addedPlugins.stream().anyMatch(p -> p.startsWith("rerun:"));

        if (!alreadyAddedRerunPlugin.test(pluginCollection)) {
            if (feature != null) {
                rerunFile = getMultiThreadRerunFile();
            } else {
                final String cucumberRerunFile = cucumberRerunPlugin.apply(courgetteProperties);
                rerunFile = cucumberRerunFile != null ? cucumberRerunFile : String.format("%s/courgette-rerun.txt", reportTargetDir);
            }
            pluginCollection.add("rerun:" + rerunFile);
        }

        if (feature != null && pluginCollection.stream().noneMatch(plugin -> plugin.startsWith("json:"))) {
            pluginCollection.add(String.format("json:%s.json", getMultiThreadReportFile()));
        }

        if (courgetteProperties.isReportPortalPluginEnabled()) {
            if (pluginCollection.stream().noneMatch(plugin -> plugin.contains(getCourgetteReportXmlForReportPortal()))) {
                pluginCollection.add("junit:" + getCourgetteReportXmlForReportPortal());
            }
        }

        if (feature != null) {
            final String junitReportPlugin = String.format("junit:%s.xml", getMultiThreadReportFile());
            if (pluginCollection.stream().noneMatch(plugin -> plugin.equals(junitReportPlugin))) {
                pluginCollection.add(junitReportPlugin);
            }

            if (courgetteProperties.shouldPersistCucumberJsonReports()) {
                final String featureReportFile = getFeatureReportFile();
                pluginCollection.add(String.format("json:%s.json", featureReportFile));
                pluginCollection.add(String.format("message:%s.ndjson", featureReportFile));
            }
        }

        checkDisabledPlugins(pluginCollection);

        return copyOf(pluginCollection.toArray(), pluginCollection.size(), String[].class);
    }

    private void checkDisabledPlugins(HashSet<String> plugins) {
        if (plugins.stream().anyMatch(p -> p.startsWith("html"))) {
            if (!courgetteProperties.isCucumberHtmlReportEnabled()) {
                plugins.removeIf(p -> p.startsWith("html") || (
                        !courgetteProperties.shouldPersistCucumberJsonReports() && p.startsWith("message"))
                );
            }
        }
    }

    private String[] addDefaultPlugins(String[] plugins) {
        plugins = Arrays.copyOf(plugins, plugins.length + 2);
        plugins[plugins.length - 1] = "json:" + getCourgetteReportJson();
        plugins[plugins.length - 2] = "message:" + getCourgetteReportNdJson();
        return plugins;
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

    private String getTempDirectory() {
        final String fileSeparator = File.separator;
        final String tmpDir = System.getProperty("java.io.tmpdir");

        if (!tmpDir.endsWith(fileSeparator)) {
            return tmpDir + fileSeparator;
        }
        return tmpDir;
    }
}