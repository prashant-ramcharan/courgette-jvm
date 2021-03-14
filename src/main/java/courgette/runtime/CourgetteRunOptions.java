package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import courgette.api.HtmlReport;
import courgette.integration.reportportal.ReportPortalProperties;
import courgette.runtime.event.CourgetteEvent;
import courgette.runtime.utils.FileUtils;
import courgette.runtime.utils.SystemPropertyUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Arrays;

public class CourgetteRunOptions implements CourgetteOptions {
    private CourgetteOptions courgetteOptions;

    public CourgetteRunOptions(Class clazz) {
        validate(clazz);
        validatePlugins();
        validateSlackOptions();
    }

    @Override
    public int threads() {
        return SystemPropertyUtils.getIntProperty(CourgetteSystemProperty.THREADS, courgetteOptions.threads());
    }

    @Override
    public CourgetteRunLevel runLevel() {
        return SystemPropertyUtils.getEnumProperty(CourgetteSystemProperty.RUN_LEVEL, CourgetteRunLevel.class, courgetteOptions.runLevel());
    }

    @Override
    public boolean rerunFailedScenarios() {
        return SystemPropertyUtils.getBoolProperty(CourgetteSystemProperty.RERUN_FAILED_SCENARIOS, courgetteOptions.rerunFailedScenarios());
    }

    @Override
    public int rerunAttempts() {
        return SystemPropertyUtils.getIntProperty(CourgetteSystemProperty.RERUN_ATTEMPTS, courgetteOptions.rerunAttempts());
    }

    @Override
    public boolean showTestOutput() {
        return SystemPropertyUtils.getBoolProperty(CourgetteSystemProperty.SHOW_TEST_OUTPUT, courgetteOptions.showTestOutput());
    }

    @Override
    public String reportTitle() {
        return SystemPropertyUtils.getNonEmptyStringProperty(CourgetteSystemProperty.REPORT_TITLE, courgetteOptions.reportTitle(), "Courgette-JVM Report");
    }

    @Override
    public String reportTargetDir() {
        return SystemPropertyUtils.getNonEmptyStringProperty(CourgetteSystemProperty.REPORT_TARGET_DIR, courgetteOptions.reportTargetDir(), "target");
    }

    @Override
    public CucumberOptions cucumberOptions() {
        return courgetteOptions.cucumberOptions();
    }

    @Override
    public String[] plugin() {
        return courgetteOptions.plugin();
    }

    @Override
    public String environmentInfo() {
        return SystemPropertyUtils.getNonEmptyStringProperty(CourgetteSystemProperty.ENVIRONMENT_INFO, courgetteOptions.environmentInfo(), "");
    }

    @Override
    public HtmlReport[] disableHtmlReport() {
        return courgetteOptions.disableHtmlReport();
    }

    @Override
    public String[] classPath() {
        return courgetteOptions.classPath();
    }

    @Override
    public String slackWebhookUrl() {
        return SystemPropertyUtils.getNonEmptyStringProperty(CourgetteSystemProperty.SLACK_WEBHOOK_URL, courgetteOptions.slackWebhookUrl(), "");
    }

    @Override
    public String[] slackChannel() {
        return courgetteOptions.slackChannel();
    }

    @Override
    public CourgetteEvent[] slackEventSubscription() {
        return courgetteOptions.slackEventSubscription();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    private void validate(Class clazz) {
        courgetteOptions = (CourgetteOptions) Arrays.stream(clazz.getDeclaredAnnotations())
                .filter(annotation -> annotation.annotationType().equals(CourgetteOptions.class))
                .findFirst()
                .orElseThrow(() -> new CourgetteException("Runner class is not annotated with @CourgetteOptions"));
    }

    private void validatePlugins() {
        if (plugin().length > 0) {
            validateReportPortalPlugin();
        }
    }

    private void validateReportPortalPlugin() {
        final String reportPortalPropertiesFilename = "reportportal.properties";

        if (Arrays.stream(courgetteOptions.plugin()).anyMatch(plugin -> plugin.equalsIgnoreCase("reportportal"))) {
            File reportPortalPropertiesFile = FileUtils.getClassPathFile(reportPortalPropertiesFilename);

            if (reportPortalPropertiesFile == null) {
                throw new CourgetteException("The " + reportPortalPropertiesFilename + " file must be in your classpath to use the Courgette reportportal plugin");
            }
            ReportPortalProperties.getInstance().validate();
        }
    }

    private void validateSlackOptions() {
        final CourgetteSlackOptions slackOptions = new CourgetteSlackOptions(
                slackWebhookUrl(), Arrays.asList(slackChannel()), Arrays.asList(slackEventSubscription()));

        if (slackOptions.shouldValidate() && !slackOptions.isValid()) {
            throw new CourgetteException("You must provide a Slack webhook URL and valid Slack channels");
        }
    }
}