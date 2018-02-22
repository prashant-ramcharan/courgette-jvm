package courgette.runtime.utils;

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
}