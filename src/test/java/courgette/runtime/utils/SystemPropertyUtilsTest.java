package courgette.runtime.utils;

import courgette.api.CourgetteRunLevel;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemPropertyUtilsTest {
    @After
    public void clearProperties() {
        System.clearProperty("courgette.test.int");
        System.clearProperty("courgette.test.bool");
        System.clearProperty("courgette.test.enum");
        System.clearProperty("courgette.test.string");
        System.clearProperty("courgette.test.array");
        System.clearProperty("courgette.test.list");
    }

    @Test
    public void returnsDefaultsForMissingOrInvalidProperties() {
        assertEquals(5, SystemPropertyUtils.getIntProperty("courgette.test.int", 5));
        System.setProperty("courgette.test.int", "not-a-number");
        assertEquals(5, SystemPropertyUtils.getIntProperty("courgette.test.int", 5));

        assertEquals(CourgetteRunLevel.FEATURE, SystemPropertyUtils.getEnumProperty("courgette.test.enum", CourgetteRunLevel.class, CourgetteRunLevel.FEATURE));
        System.setProperty("courgette.test.enum", "missing");
        assertEquals(CourgetteRunLevel.FEATURE, SystemPropertyUtils.getEnumProperty("courgette.test.enum", CourgetteRunLevel.class, CourgetteRunLevel.FEATURE));
    }

    @Test
    public void parsesTypedProperties() {
        System.setProperty("courgette.test.int", " 12 ");
        System.setProperty("courgette.test.bool", " true ");
        System.setProperty("courgette.test.enum", "scenario");

        assertEquals(12, SystemPropertyUtils.getIntProperty("courgette.test.int", 5));
        assertTrue(SystemPropertyUtils.getBoolProperty("courgette.test.bool", false));
        assertFalse(SystemPropertyUtils.getBoolProperty("missing", false));
        assertEquals(CourgetteRunLevel.SCENARIO, SystemPropertyUtils.getEnumProperty("courgette.test.enum", CourgetteRunLevel.class, CourgetteRunLevel.FEATURE));
    }

    @Test
    public void parsesDistinctTrimmedStringArrays() {
        System.setProperty("courgette.test.array", " one, two,one ");

        assertArrayEquals(new String[]{"one", "two"}, SystemPropertyUtils.getStringArrayProperty("courgette.test.array", new String[]{"default"}));
    }

    @Test
    public void splitsWhitespaceSeparatedPropertiesIntoLists() {
        System.setProperty("courgette.test.list", " -Xmx256m   -Dfoo=bar ");
        List<String> values = new ArrayList<>();

        SystemPropertyUtils.splitAndAddPropertyToList("courgette.test.list", values);

        assertEquals(java.util.Arrays.asList("-Xmx256m", "-Dfoo=bar"), values);
    }
}
