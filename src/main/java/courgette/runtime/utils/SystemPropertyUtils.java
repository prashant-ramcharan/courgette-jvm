package courgette.runtime.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public final class SystemPropertyUtils {

    public static int getIntProperty(String key, int defaultValue) {
        Object value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static boolean getBoolProperty(String key, boolean defaultValue) {
        Object value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.valueOf(value.toString().trim());
    }

    public static <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        Object value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.toString().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }

    public static String getNonEmptyStringProperty(String key, String providedValue, String defaultValue) {
        final Predicate<String> isEmptyString = (string) -> string == null || string.trim().isEmpty();

        String value = System.getProperty(key);

        if (isEmptyString.test(value)) {
            if (!isEmptyString.test(providedValue)) {
                return providedValue;
            } else {
                return defaultValue;
            }
        }
        return value;
    }

    public static String[] getStringArrayProperty(String key, String[] defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }

        String[] splitValue = val.split(",");
        Object[] value = Arrays.stream(splitValue).map(String::trim).distinct().toArray();
        return Arrays.copyOf(value, value.length, String[].class);
    }

    @SuppressWarnings("unchecked")
    public static <T> void splitAndAddPropertyToList(String key, List<T> list) {
        Object value = System.getProperty(key);
        if (value != null) {
            String valueAsString = value.toString().replaceAll("\\s{2,}", " ").trim();

            if (valueAsString.length() > 0) {
                Arrays.asList(valueAsString.split(" ")).forEach(t -> list.add((T) t));
            }
        }
    }
}