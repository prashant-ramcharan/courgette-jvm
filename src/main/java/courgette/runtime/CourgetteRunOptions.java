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
        return SystemPropertyUtils.getIntProperty("courgette.threads", courgetteOptions.threads());
    }

    @Override
    public CourgetteRunLevel runLevel() {
        return SystemPropertyUtils.getEnumProperty("courgette.runLevel", CourgetteRunLevel.class, courgetteOptions.runLevel());
    }

    @Override
    public boolean rerunFailedScenarios() {
        return SystemPropertyUtils.getBoolProperty("courgette.rerunFailedScenarios", courgetteOptions.rerunFailedScenarios());
    }

    @Override
    public boolean showTestOutput() {
        return SystemPropertyUtils.getBoolProperty("courgette.showTestOutput", courgetteOptions.showTestOutput());
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