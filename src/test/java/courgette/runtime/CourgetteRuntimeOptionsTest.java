package courgette.runtime;

import courgette.api.CourgetteOptions;
import courgette.api.CucumberOptions;
import io.cucumber.core.snippets.SnippetType;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CourgetteRuntimeOptionsTest {
    @After
    public void clearSystemProperties() {
        System.clearProperty("cucumber.features");
        System.clearProperty("cucumber.tags");
        System.clearProperty("cucumber.glue");
    }

    @Test
    public void mapsAnnotatedCucumberOptionsToCommandLineArguments() {
        CourgetteRuntimeOptions options = new CourgetteRuntimeOptions(propertiesFor(RuntimeOptionRunner.class));

        Map<String, List<String>> mappedOptions = options.mapRuntimeOptions();

        assertEquals(list("--glue", "steps"), mappedOptions.get("--glue"));
        assertEquals(list("--glue", "hooks"), mappedOptions.get("--extraGlue"));
        assertEquals(list("--tags", "@regression"), mappedOptions.get("--filter.tags"));
        assertEquals(list("--name", "checkout"), mappedOptions.get("--name"));
        assertEquals(list("--snippets", SnippetType.CAMELCASE.name().toLowerCase()), mappedOptions.get("--snippets"));
        assertEquals(list("--dry-run"), mappedOptions.get("--dryRun"));
        assertEquals(list("--monochrome"), mappedOptions.get("--monochrome"));
        assertEquals(list("features/a.feature", "features/b.feature"), mappedOptions.get(null));
    }

    @Test
    public void systemPropertiesOverrideCucumberOptions() {
        System.setProperty("cucumber.features", "features/override.feature");
        System.setProperty("cucumber.tags", "@smoke, not @wip");
        System.setProperty("cucumber.glue", "override.steps");

        CourgetteRuntimeOptions options = new CourgetteRuntimeOptions(propertiesFor(RuntimeOptionRunner.class));
        Map<String, List<String>> mappedOptions = options.mapRuntimeOptions();

        assertEquals(list("features/override.feature"), mappedOptions.get(null));
        assertEquals(list("--tags", "@smoke", "--tags", "not @wip"), mappedOptions.get("--filter.tags"));
        assertEquals(list("--glue", "override.steps"), mappedOptions.get("--glue"));
    }

    @Test
    public void exposesConfiguredReportPlugins() {
        CourgetteRuntimeOptions options = new CourgetteRuntimeOptions(propertiesFor(RuntimeOptionRunner.class));

        assertTrue(options.getReportFiles().contains("build/cucumber.json"));
        assertTrue(options.getReportFiles().contains("build/cucumber.ndjson"));
        assertTrue(options.getReportFiles().contains("build/cucumber.xml"));
    }

    private CourgetteProperties propertiesFor(Class<?> clazz) {
        return new CourgetteProperties(new CourgetteRunOptions(clazz), "session", 1);
    }

    private List<String> list(String... values) {
        return java.util.Arrays.asList(values);
    }

    @CourgetteOptions(cucumberOptions = @CucumberOptions(
            features = {"features/a.feature", "features/b.feature"},
            glue = "steps",
            extraGlue = "hooks",
            tags = "@regression",
            plugin = {"pretty", "json:build/cucumber.json", "message:build/cucumber.ndjson", "junit:build/cucumber.xml"},
            name = "checkout",
            dryRun = true,
            monochrome = true,
            snippets = SnippetType.CAMELCASE))
    private static class RuntimeOptionRunner {
    }
}
