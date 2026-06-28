package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import courgette.api.HtmlReport;
import courgette.api.MobileDeviceType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourgettePropertiesTest {
    @Test
    public void countsDistinctSimulatorAndRealDevices() {
        CourgetteProperties properties = propertiesFor(FullOptions.class);

        assertEquals(1, properties.getSimulatorDeviceCount());
        assertEquals(2, properties.getRealDeviceCount());
        assertEquals(1, properties.getMaxThreadsFromMobileDevices(DeviceType.SIMULATOR));
        assertEquals(2, properties.getMaxThreadsFromMobileDevices(DeviceType.REAL_DEVICE));
    }

    @Test
    public void detectsEnabledPluginsAndOptions() {
        CourgetteProperties properties = propertiesFor(FullOptions.class);

        assertTrue(properties.isExtentReportsPluginEnabled());
        assertTrue(properties.isMobileDeviceAllocationPluginEnabled());
        assertFalse(properties.isReportPortalPluginEnabled());
        assertTrue(properties.isFeatureRunLevel());
        assertTrue(properties.shouldPersistCucumberJsonReports());
        assertTrue(properties.useCustomClasspath());
        assertTrue(properties.isMultipleMobileDeviceTypes());
        assertTrue(properties.isRerunEnabled());
    }

    @Test
    public void honoursDisabledHtmlReportOptions() {
        CourgetteProperties properties = propertiesFor(FullOptions.class);

        assertFalse(properties.isCourgetteHtmlReportEnabled());
        assertFalse(properties.isCucumberHtmlReportEnabled());
    }

    @Test
    public void usesFixedDelayBeforeRandomDelay() {
        CourgetteProperties properties = propertiesFor(FullOptions.class);

        assertEquals(25, properties.threadDelay());
    }

    @Test
    public void detectsCucumberXmlPlugin() {
        CourgetteProperties properties = propertiesFor(FullOptions.class);

        assertTrue(properties.isCucumberXmlReportPluginEnabled());
    }

    private CourgetteProperties propertiesFor(Class<?> clazz) {
        return new CourgetteProperties(new CourgetteRunOptions(clazz), "session", 4);
    }

    @CourgetteOptions(
            threads = 4,
            runLevel = CourgetteRunLevel.FEATURE,
            rerunFailedScenarios = true,
            rerunAttempts = 2,
            plugin = {CourgettePlugin.EXTENT_REPORTS, CourgettePlugin.MOBILE_DEVICE_ALLOCATOR},
            disableHtmlReport = HtmlReport.COURGETTE_AND_CUCUMBER_HTML,
            persistParallelCucumberJsonReports = true,
            classPath = "build/test-classes",
            mobileDeviceType = MobileDeviceType.SIMULATOR_AND_REAL_DEVICE,
            mobileDevice = {"iPhone 14", "iphone 14", "Pixel:abc", "Galaxy:def"},
            realMobileDeviceTag = "@real",
            fixedThreadDelay = 25,
            randomThreadDelay = 100,
            cucumberOptions = @CucumberOptions(plugin = "junit:build/cucumber.xml"))
    private static class FullOptions {
    }
}
