package courgette.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourgetteEnvironmentInfo {
    private final String environmentInfo;

    public CourgetteEnvironmentInfo(String environmentInfo) {
        this.environmentInfo = environmentInfo;
    }

    public List<String> list() {
        final List<String> environment = new ArrayList<>();

        final String[] values = environmentInfo.trim().split(";");

        for (String value : values) {
            String[] keyValue = value.trim().split("=");
            if (keyValue.length == 2) {
                environment.add(keyValue[0].trim() + " = " + keyValue[1].trim());
            }
        }
        return environment;
    }

    public Map<String, String> map() {
        Map<String, String> environment = new HashMap<>();
        list().forEach(info -> {
            String[] keyPair = info.split(("="));
            environment.put(keyPair[0].trim(), keyPair[1].trim());
        });
        return environment;
    }

    public LinkedHashMap<String, String> defaultEnvironment() {
        LinkedHashMap<String, String> environment = createDefaultEnvironment();
        environment.putAll(map());
        return environment;
    }

    private LinkedHashMap<String, String> createDefaultEnvironment() {
        LinkedHashMap<String, String> defaultEnvironment = new LinkedHashMap<>();
        defaultEnvironment.put("os_name", System.getProperty("os.name"));
        defaultEnvironment.put("os_arch", System.getProperty("os.arch"));
        defaultEnvironment.put("java_version", System.getProperty("java.version"));
        return defaultEnvironment;
    }
}
