package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import courgette.api.MobileDeviceType;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CourgetteRunOptionsTest {
    @After
    public void clearSystemProperties() {
        System.clearProperty(CourgetteSystemProperty.THREADS);
        System.clearProperty(CourgetteSystemProperty.RUN_LEVEL);
        System.clearProperty(CourgetteSystemProperty.REPORT_TARGET_DIR);
        System.clearProperty(CourgetteSystemProperty.MOBILE_DEVICE);
    }

    @Test
    public void readsAnnotationDefaultsAndNormalisesReportTargetDirectory() {
        CourgetteRunOptions options = new CourgetteRunOptions(BasicRunner.class);

        assertEquals(3, options.threads());
        assertEquals(CourgetteRunLevel.SCENARIO, options.runLevel());
        assertEquals("target", options.reportTargetDir());
        assertArrayEquals(new String[]{"features"}, options.cucumberOptions().features());
    }

    @Test
    public void systemPropertiesOverrideRuntimeOptions() {
        System.setProperty(CourgetteSystemProperty.THREADS, "7");
        System.setProperty(CourgetteSystemProperty.RUN_LEVEL, "feature");
        System.setProperty(CourgetteSystemProperty.REPORT_TARGET_DIR, "build/reports");
        System.setProperty(CourgetteSystemProperty.MOBILE_DEVICE, "iPhone, Pixel:123");

        CourgetteRunOptions options = new CourgetteRunOptions(BasicRunner.class);

        assertEquals(7, options.threads());
        assertEquals(CourgetteRunLevel.FEATURE, options.runLevel());
        assertEquals("build/reports", options.reportTargetDir());
        assertArrayEquals(new String[]{"iPhone", "Pixel:123"}, options.mobileDevice());
    }

    @Test(expected = CourgetteException.class)
    public void rejectsClassesWithoutCourgetteOptions() {
        new CourgetteRunOptions(NoOptions.class);
    }

    @Test(expected = CourgetteException.class)
    public void validatesMobileDeviceAllocatorDevices() {
        new CourgetteRunOptions(InvalidMobileDeviceRunner.class);
    }

    @CourgetteOptions(
            threads = 3,
            runLevel = CourgetteRunLevel.SCENARIO,
            reportTargetDir = "",
            mobileDevice = "android",
            cucumberOptions = @CucumberOptions(features = "features"))
    private static class BasicRunner {
    }

    private static class NoOptions {
    }

    @CourgetteOptions(
            plugin = CourgettePlugin.MOBILE_DEVICE_ALLOCATOR,
            mobileDeviceType = MobileDeviceType.REAL_DEVICE,
            mobileDevice = "simulator-only",
            cucumberOptions = @CucumberOptions(features = "features"))
    private static class InvalidMobileDeviceRunner {
    }
}
