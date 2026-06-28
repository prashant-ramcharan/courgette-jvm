import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.CourgetteTestOutput;
import courgette.api.CucumberOptions;
import courgette.api.MobileDeviceType;
import courgette.api.junit.Courgette;
import org.junit.runner.RunWith;

@RunWith(Courgette.class)
@CourgetteOptions(
        threads = 10,
        runLevel = CourgetteRunLevel.SCENARIO,
        rerunFailedScenarios = true,
        testOutput = CourgetteTestOutput.CONSOLE,
        reportTargetDir = "build",
        environmentInfo = "browser=chrome; git_branch=master; project_info=Courgette-JVM is awesome!",
        plugin = CourgettePlugin.MOBILE_DEVICE_ALLOCATOR,
        mobileDeviceType = MobileDeviceType.SIMULATOR_AND_REAL_DEVICE,
        mobileDevice = {"iphone8", "ipad:123"},
        realMobileDeviceTag = {"@upload"},
        generateCourgetteRunLog = true,
        cucumberOptions = @CucumberOptions(
                features = "src/test/resources/features/integration",
                glue = "steps",
                tags = "@regression and not @excluded",
                publish = false,
                plugin = {
                        "pretty",
                        "json:build/cucumber-report/cucumber.json",
                        "html:build/cucumber-report/cucumber.html",
                        "junit:build/cucumber-report/cucumber.xml",
                        "message:build/cucumber-report/cucumber.ndjson"}
        ))
public class ScenarioSuite {
}
