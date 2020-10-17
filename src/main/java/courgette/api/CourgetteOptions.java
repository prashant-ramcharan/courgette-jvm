package courgette.api;

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
     * @return the number of re-run attempts
     */
    int rerunAttempts() default 1;

    /**
     * @return true to show test output
     */
    boolean showTestOutput() default false;

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
     * @return the Cucumber options
     */
    CucumberOptions cucumberOptions();
}
