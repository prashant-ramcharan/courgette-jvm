package courgette.runtime.junit;

import io.cucumber.core.backend.ObjectFactory;

public class NoObjectFactory implements ObjectFactory {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean addClass(Class<?> glueClass) {
        return false;
    }

    @Override
    public <T> T getInstance(Class<T> glueClass) {
        return null;
    }
}
