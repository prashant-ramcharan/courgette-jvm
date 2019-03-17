package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.runtime.utils.SystemPropertyUtils;
import cucumber.api.CucumberOptions;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public class CourgetteRunOptions implements CourgetteOptions {
    private CourgetteOptions courgetteOptions;

    public CourgetteRunOptions(Class clazz) {
        validate(clazz);
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
    public String reportTargetDir() {
        return SystemPropertyUtils.getNonEmptyStringProperty(CourgetteSystemProperty.REPORT_TARGET_DIR, courgetteOptions.reportTargetDir(), "target");
    }

    @Override
    public CucumberOptions cucumberOptions() {
        return courgetteOptions.cucumberOptions();
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
}