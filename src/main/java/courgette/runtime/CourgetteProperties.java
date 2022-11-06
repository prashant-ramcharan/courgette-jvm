package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.HtmlReport;
import courgette.runtime.utils.SystemPropertyUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CourgetteProperties {
    private final CourgetteOptions courgetteOptions;
    private final String sessionId;
    private final Instant sessionStartTime;
    private final Integer maxThreads;

    public CourgetteProperties(CourgetteOptions courgetteOptions, String sessionId, Integer maxThreads) {
        this.courgetteOptions = courgetteOptions;
        this.sessionId = sessionId;
        this.sessionStartTime = Instant.now();
        this.maxThreads = maxThreads;
    }

    public CourgetteOptions getCourgetteOptions() {
        return courgetteOptions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getSessionStartTime() {
        return sessionStartTime;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public int getMaxThreadsFromMobileDevices() {
        return Arrays.stream(courgetteOptions.mobileDevice())
                .distinct()
                .map(device -> device.toLowerCase().trim())
                .collect(Collectors.toSet()).size();
    }

    public boolean isReportPortalPluginEnabled() {
        return Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase(CourgettePlugin.REPORT_PORTAL));
    }

    public boolean isExtentReportsPluginEnabled() {
        return Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase(CourgettePlugin.EXTENT_REPORTS));
    }

    public boolean isMobileDeviceAllocationPluginEnabled() {
        return Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase(CourgettePlugin.MOBILE_DEVICE_ALLOCATOR));
    }

    public boolean isCucumberReportPublisherEnabled() {
        return SystemPropertyUtils.getBoolProperty("cucumber.publish", courgetteOptions.cucumberOptions().publish());
    }

    public boolean isFeatureRunLevel() {
        return CourgetteRunLevel.FEATURE.equals(courgetteOptions.runLevel());
    }

    public boolean isCourgetteHtmlReportEnabled() {
        return checkIfReportIsEnabled.test(HtmlReport.COURGETTE_HTML);
    }

    public boolean isCucumberHtmlReportEnabled() {
        return checkIfReportIsEnabled.test(HtmlReport.CUCUMBER_HTML);
    }

    public boolean shouldPersistCucumberJsonReports() {
        return courgetteOptions.persistParallelCucumberJsonReports();
    }

    public boolean useCustomClasspath() {
        return courgetteOptions.classPath().length > 0;
    }

    public CourgetteSlackOptions slackOptions() {
        return new CourgetteSlackOptions(courgetteOptions.slackWebhookUrl(),
                Arrays.asList(courgetteOptions.slackChannel()),
                courgetteOptions.slackTestId(),
                Arrays.asList(courgetteOptions.slackEventSubscription()));
    }

    public boolean publishEventsToSlack() {
        return slackOptions().isValid();
    }

    public int threadDelay() {
        if (courgetteOptions.fixedThreadDelay() > 0) {
            return courgetteOptions.fixedThreadDelay();
        } else if (courgetteOptions.randomThreadDelay() > 0) {
            return new Random().nextInt(courgetteOptions.randomThreadDelay());
        }
        return 0;
    }

    private final Predicate<HtmlReport> checkIfReportIsEnabled = (report) ->
            Arrays.stream(getCourgetteOptions().disableHtmlReport()).noneMatch(r -> r.equals(HtmlReport.COURGETTE_AND_CUCUMBER_HTML) || r.equals(report));
}
