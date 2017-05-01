package courgette.api;

import cucumber.api.CucumberOptions;

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
     * @return true to re-run failed scenarios
     */
    boolean rerunFailedScenarios() default false;

    /**
     * @return the Cucumber options
     */
    CucumberOptions cucumberOptions();
}
