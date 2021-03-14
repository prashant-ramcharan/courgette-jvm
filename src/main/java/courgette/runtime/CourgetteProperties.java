package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.api.HtmlReport;
import courgette.runtime.utils.SystemPropertyUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.function.Predicate;

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

    public boolean isReportPortalPluginEnabled() {
        return Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase("reportportal"));
    }

    public boolean isExtentReportsPluginEnabled() {
        return Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase("extentreports"));
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

    public boolean useCustomClasspath() {
        return courgetteOptions.classPath().length > 0;
    }

    public CourgetteSlackOptions slackOptions() {
        return new CourgetteSlackOptions(courgetteOptions.slackWebhookUrl(),
                Arrays.asList(courgetteOptions.slackChannel()),
                Arrays.asList(courgetteOptions.slackEventSubscription()));
    }

    public boolean publishEventsToSlack() {
        return slackOptions().isValid();
    }

    private final Predicate<HtmlReport> checkIfReportIsEnabled = (report) -> !Arrays.asList(getCourgetteOptions().disableHtmlReport()).contains(report);
}
