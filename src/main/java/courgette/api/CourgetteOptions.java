package courgette.api;

import courgette.runtime.event.CourgetteEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CourgetteOptions {
    /**
     * @return the number of parallel threads
     */
    int threads() default 5;

    /**
     * @return the run level (feature or scenario level)
     */
    CourgetteRunLevel runLevel() default CourgetteRunLevel.FEATURE;

    /**
     * @return true to re-run failed scenarios
     */
    boolean rerunFailedScenarios() default false;

    /**
     * @return the features to exclude from re-running when tests fail
     */
    String[] excludeFeatureFromRerun() default {};

    /**
     * @return the tags to exclude from re-running when tests fail
     */
    String[] excludeTagFromRerun() default {};

    /**
     * @return the number of re-run attempts
     */
    int rerunAttempts() default 1;

    /**
     * @return the test output for each test run
     */
    CourgetteTestOutput testOutput() default CourgetteTestOutput.DISCARD;

    /**
     * @return the reportTitle for the Courgette Html report
     */
    String reportTitle() default "Courgette-JVM Report";

    /**
     * @return target directory of courgette-report (this defaults to 'target' directory)
     */
    String reportTargetDir() default "";

    /**
     * @return the collection of Courgette plugin
     */
    String[] plugin() default {};

    /**
     * @return custom environment information
     */
    String environmentInfo() default "";

    /**
     * @return the collection of disabled html reports
     */
    HtmlReport[] disableHtmlReport() default {};

    /**
     * @return true to save Cucumber json and ndjson reports for each test run
     */
    boolean persistParallelCucumberJsonReports() default false;

    /**
     * @return custom classpath
     */
    String[] classPath() default {};

    /**
     * @return the Slack webhook URL
     */
    String slackWebhookUrl() default "";

    /**
     * @return the Slack channels
     */
    String[] slackChannel() default {};

    /**
     * @return the Slack test id
     */
    String slackTestId() default "";

    /**
     * @return the Courgette events subscriptions
     */
    CourgetteEvent[] slackEventSubscription() default {};

    /**
     * @return the mobile device type
     */
    MobileDeviceType mobileDeviceType() default MobileDeviceType.SIMULATOR;

    /**
     * @return the mobile devices for device allocation
     */
    String[] mobileDevice() default {};

    /**
     * @return the matching Cucumber tags for real mobile device allocation
     */
    String[] realMobileDeviceTag() default {};

    /**
     * @return the time in milliseconds for the fixed thread delay
     */
    int fixedThreadDelay() default 0;

    /**
     * @return the time in milliseconds for the random thread delay
     */
    int randomThreadDelay() default 0;

    /**
     * @return true to generate a Courgette run log file
     */
    boolean generateCourgetteRunLog() default false;

    /**
     * @return the Cucumber options
     */
    CucumberOptions cucumberOptions();
}
