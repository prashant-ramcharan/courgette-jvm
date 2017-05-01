package courgette.runtime.utils;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

public final class ReflectionUtils {

    public static BiFunction<Object, String, Object> classField = (obj, field) -> {
        Field classField = null;
        try {
            classField = obj.getClass().getDeclaredField(field);
            classField.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (classField != null) {
            try {
                return classField.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    };
}
