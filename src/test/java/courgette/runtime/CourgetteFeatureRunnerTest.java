package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.CourgetteTestOutput;
import courgette.api.CucumberOptions;
import courgette.api.MobileDeviceType;
import io.cucumber.core.gherkin.Feature;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CourgetteFeatureRunnerTest {
    @After
    public void clearSystemProperties() {
        System.clearProperty(CourgetteSystemProperty.VM_OPTIONS);
        System.clearProperty("cucumber.publish.enabled");
        System.clearProperty("cucumber.tags");
        System.clearProperty("CUCUMBER_PUBLISH_TOKEN");
    }

    @Test
    public void buildsCucumberCliCommandWithVmOptionsAndCustomClasspath() {
        System.setProperty(CourgetteSystemProperty.VM_OPTIONS, "-Xmx256m -Dcustom.option=true");
        System.setProperty("CUCUMBER_PUBLISH_TOKEN", "secret");
        Fixture fixture = fixture(CustomClasspathRunner.class);
        System.setProperty("cucumber.tags", "@shouldBeRemoved");

        ProcessBuilder builder = featureRunner(fixture).new Builder().buildProcess();
        List<String> command = builder.command();

        assertEquals("java", command.get(0));
        assertTrue(command.contains("-Xmx256m"));
        assertTrue(command.contains("-Dcustom.option=true"));
        assertTrue(command.contains("-cp"));
        assertTrue(command.contains("build/classes/java/main" + File.pathSeparator + "build/resources/main"));
        assertTrue(command.contains("io.cucumber.core.cli.Main"));
        assertTrue(command.stream().anyMatch(value -> value.endsWith("src/test/resources/features/integration/local-web-app.feature")));
        assertFalse(command.stream().anyMatch(value -> value.startsWith("-Djava.class.path")));
        assertFalse(command.stream().anyMatch(value -> value.startsWith("-Dcucumber.tags")));
        assertFalse(command.stream().anyMatch(value -> value.contains("CUCUMBER_PUBLISH_TOKEN")));
        assertTrue(command.contains("-Dcucumber.publish.enabled=false"));
        assertTrue(command.contains("-Dcucumber.publish.quiet=true"));
    }

    @Test
    public void addsMobileDeviceAllocatorSystemProperties() {
        Fixture fixture = fixture(MobileDeviceRunner.class);

        CourgetteFeatureRunner featureRunner = featureRunner(fixture);
        ProcessBuilder builder = featureRunner.new Builder().buildProcess();
        List<String> command = builder.command();

        assertTrue(command.contains("-D" + CourgetteSystemProperty.DEVICE_NAME_SYSTEM_PROPERTY + "=Pixel"));
        assertTrue(command.stream().anyMatch(value -> value.startsWith("-D" + CourgetteSystemProperty.PARALLEL_PORT_SYSTEM_PROPERTY + "=")));
        assertTrue(command.contains("-D" + CourgetteSystemProperty.UDID_SYSTEM_PROPERTY + "=abc123"));
        assertNotNull(featureRunner.new Builder());
    }

    @Test
    public void redirectsFileOutputToReportTargetDirectory() {
        Fixture fixture = fixture(FileOutputRunner.class);

        ProcessBuilder builder = featureRunner(fixture).new Builder().buildProcess();

        assertEquals(ProcessBuilder.Redirect.Type.WRITE, builder.redirectOutput().type());
        assertTrue(builder.redirectOutput().file().getPath().contains("build/courgette-test-output"));
        assertTrue(builder.redirectOutput().file().getName().endsWith("_session.log"));
    }

    private CourgetteFeatureRunner featureRunner(Fixture fixture) {
        Map<String, List<String>> args = fixture.runnerInfo.getRuntimeOptions();
        return new CourgetteFeatureRunner(
                fixture.runnerInfo,
                args,
                fixture.properties,
                new CourgettePluginService(new CourgetteMobileDeviceAllocatorService(fixture.properties.getCourgetteOptions().mobileDevice())));
    }

    private Fixture fixture(Class<?> runnerClass) {
        CourgetteProperties properties = new CourgetteProperties(new CourgetteRunOptions(runnerClass), "session", 1);
        Feature feature = new CourgetteLoader(properties).getFeatures().get(0);
        return new Fixture(properties, new CourgetteRunnerInfo(properties, feature, null));
    }

    private static class Fixture {
        private final CourgetteProperties properties;
        private final CourgetteRunnerInfo runnerInfo;

        private Fixture(CourgetteProperties properties, CourgetteRunnerInfo runnerInfo) {
            this.properties = properties;
            this.runnerInfo = runnerInfo;
        }
    }

    @CourgetteOptions(
            classPath = {"build/classes/java/main", "build/resources/main"},
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression"))
    private static class CustomClasspathRunner {
    }

    @CourgetteOptions(
            runLevel = CourgetteRunLevel.FEATURE,
            plugin = CourgettePlugin.MOBILE_DEVICE_ALLOCATOR,
            mobileDeviceType = MobileDeviceType.REAL_DEVICE,
            mobileDevice = "Pixel:abc123",
            realMobileDeviceTag = "@upload",
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app-upload.feature",
                    glue = "steps",
                    tags = "@upload"))
    private static class MobileDeviceRunner {
    }

    @CourgetteOptions(
            testOutput = CourgetteTestOutput.FILE,
            reportTargetDir = "build",
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression"))
    private static class FileOutputRunner {
    }
}
