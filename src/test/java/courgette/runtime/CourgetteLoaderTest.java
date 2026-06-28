package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import io.cucumber.core.gherkin.Feature;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CourgetteLoaderTest {
    @After
    public void clearOverrides() {
        System.clearProperty("cucumber.tags");
    }

    @Test
    public void loadsOnlyFeaturesWithMatchingPickles() {
        CourgetteLoader loader = new CourgetteLoader(propertiesFor(IncludedRunner.class));

        List<Feature> features = loader.getFeatures();

        assertEquals(1, features.size());
        assertEquals("Filtering fixture", features.get(0).getName().orElse(""));
    }

    @Test
    public void returnsOnlyMatchingScenarioLocations() {
        CourgetteLoader loader = new CourgetteLoader(propertiesFor(IncludedRunner.class));

        Map<CucumberPickleLocation, Feature> scenarios = loader.getCucumberScenarios();

        assertEquals(1, scenarios.size());
        assertEquals(5, scenarios.keySet().iterator().next().getLine());
    }

    @Test
    public void honorsSystemPropertyTagOverrides() {
        System.setProperty("cucumber.tags", "@excluded");

        CourgetteLoader loader = new CourgetteLoader(propertiesFor(IncludedRunner.class));

        assertEquals(9, loader.getCucumberScenarios().keySet().iterator().next().getLine());
    }

    private CourgetteProperties propertiesFor(Class<?> clazz) {
        return new CourgetteProperties(new CourgetteRunOptions(clazz), "session", 1);
    }

    @CourgetteOptions(
            runLevel = CourgetteRunLevel.SCENARIO,
            cucumberOptions = @CucumberOptions(
                    features = "src/test/resources/features/fixtures/filtering.feature",
                    tags = "@included"))
    private static class IncludedRunner {
    }
}
