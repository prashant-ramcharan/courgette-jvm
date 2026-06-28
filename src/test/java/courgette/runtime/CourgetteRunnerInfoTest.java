package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgettePlugin;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import courgette.api.MobileDeviceType;
import io.cucumber.core.gherkin.Feature;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourgetteRunnerInfoTest {
    @Test
    public void scenarioRunLevelAddsLineToFeaturePath() {
        Fixture fixture = fixture(ScenarioRunner.class);
        CourgetteRunnerInfo runnerInfo = new CourgetteRunnerInfo(fixture.properties, fixture.feature, 4);

        Map<String, List<String>> runtimeOptions = runnerInfo.getRuntimeOptions();

        assertTrue(runtimeOptions.get(null).get(0).endsWith("src/test/resources/features/integration/local-web-app.feature:4"));
    }

    @Test
    public void rerunRuntimeOptionsReplaceFeaturePathAndRemoveTags() {
        Fixture fixture = fixture(ScenarioRunner.class);
        CourgetteRunnerInfo runnerInfo = new CourgetteRunnerInfo(fixture.properties, fixture.feature, 4);

        Map<String, List<String>> runtimeOptions = runnerInfo.getRerunRuntimeOptions("rerun.feature:12");

        assertEquals("rerun.feature:12", runtimeOptions.get(null).get(0));
        assertFalse(runtimeOptions.containsKey("--tags"));
    }

    @Test
    public void excludesRerunsByFeatureNameAndTag() {
        Fixture featureExcluded = fixture(FeatureExcludedRunner.class);
        Fixture tagExcluded = fixture(TagExcludedRunner.class);

        assertFalse(new CourgetteRunnerInfo(featureExcluded.properties, featureExcluded.feature, null).allowRerun());
        assertFalse(new CourgetteRunnerInfo(tagExcluded.properties, tagExcluded.feature, null).allowRerun());
    }

    @Test
    public void determinesRealDeviceForTaggedFeature() {
        Fixture fixture = fixture(MixedMobileDeviceRunner.class);

        CourgetteRunnerInfo runnerInfo = new CourgetteRunnerInfo(fixture.properties, fixture.feature, null);

        assertEquals(DeviceType.REAL_DEVICE, runnerInfo.getDeviceType());
        assertTrue(runnerInfo.allowRerun());
    }

    private Fixture fixture(Class<?> runnerClass) {
        CourgetteProperties properties = new CourgetteProperties(new CourgetteRunOptions(runnerClass), "session", 1);
        Feature feature = new CourgetteLoader(properties).getFeatures().get(0);
        return new Fixture(properties, feature);
    }

    private static class Fixture {
        private final CourgetteProperties properties;
        private final Feature feature;

        private Fixture(CourgetteProperties properties, Feature feature) {
            this.properties = properties;
            this.feature = feature;
        }
    }

    @CourgetteOptions(
            runLevel = CourgetteRunLevel.SCENARIO,
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression"))
    private static class ScenarioRunner {
    }

    @CourgetteOptions(
            rerunFailedScenarios = true,
            excludeFeatureFromRerun = "local-web-app",
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app.feature",
                    glue = "steps",
                    tags = "@regression"))
    private static class FeatureExcludedRunner {
    }

    @CourgetteOptions(
            rerunFailedScenarios = true,
            excludeTagFromRerun = "@upload",
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app-upload.feature",
                    glue = "steps",
                    tags = "@upload"))
    private static class TagExcludedRunner {
    }

    @CourgetteOptions(
            runLevel = CourgetteRunLevel.FEATURE,
            rerunFailedScenarios = true,
            plugin = CourgettePlugin.MOBILE_DEVICE_ALLOCATOR,
            mobileDeviceType = MobileDeviceType.SIMULATOR_AND_REAL_DEVICE,
            mobileDevice = {"iPhone", "Pixel:abc123"},
            realMobileDeviceTag = "@upload",
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/integration/local-web-app-upload.feature",
                    glue = "steps",
                    tags = "@upload"))
    private static class MixedMobileDeviceRunner {
    }
}
