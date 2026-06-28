import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.api.CourgetteTestOutput;
import courgette.api.CucumberOptions;
import courgette.api.junit.Courgette;
import org.junit.runner.RunWith;

@RunWith(Courgette.class)
@CourgetteOptions(
        threads = 2,
        runLevel = CourgetteRunLevel.FEATURE,
        rerunFailedScenarios = true,
        testOutput = CourgetteTestOutput.CONSOLE,
        reportTargetDir = "build/feature-suite",
        environmentInfo = "suite=feature; project_info=Courgette-JVM feature-level integration test",
        generateCourgetteRunLog = true,
        cucumberOptions = @CucumberOptions(
                features = "src/test/resources/features/integration",
                glue = "steps",
                tags = "@regression and not @excluded",
                publish = false,
                plugin = {
                        "pretty",
                        "json:build/feature-suite/cucumber-report/cucumber.json",
                        "html:build/feature-suite/cucumber-report/cucumber.html",
                        "junit:build/feature-suite/cucumber-report/cucumber.xml",
                        "message:build/feature-suite/cucumber-report/cucumber.ndjson"}
        ))
public class FeatureSuite {
}
