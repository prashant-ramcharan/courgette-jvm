package courgette.runtime;

import courgette.api.CourgetteAfterAll;
import courgette.api.CourgetteBeforeAll;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CourgetteCallbacks {
    private final Class<?> clazz;

    public CourgetteCallbacks(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void beforeAll() {
        getCallbacksForAnnotation(CourgetteBeforeAll.class).stream()
                .sorted(Comparator.comparingInt(method -> method.getAnnotation(CourgetteBeforeAll.class).order()))
                .forEachOrdered(method -> invokeCallback(method));
    }

    public void afterAll() {
        getCallbacksForAnnotation(CourgetteAfterAll.class).stream()
                .sorted(Comparator.comparingInt(method -> method.getAnnotation(CourgetteAfterAll.class).order()))
                .forEachOrdered(method -> invokeCallback(method));
    }

    private List<Method> getCallbacksForAnnotation(Class<? extends Annotation> annotation) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers())
                        && method.isAnnotationPresent(annotation)
                        && method.getParameterCount() == 0)
                .collect(Collectors.toList());
    }

    private void invokeCallback(Method callback, Object... args) {
        try {
            callback.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CourgetteException(e);
        }
    }
}
