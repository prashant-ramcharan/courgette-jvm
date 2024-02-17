package courgette.runtime;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printError;

public class CourgetteMobileDeviceAllocatorService {

    private final CopyOnWriteArrayList<CourgetteMobileDevice> availableDevices = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<CourgetteMobileDevice> unavailableDevices = new CopyOnWriteArrayList<>();

    public CourgetteMobileDeviceAllocatorService(final String[] devices) {
        availableDevices.addAll(createCourgetteMobileDevices(devices));
    }

    public synchronized CourgetteMobileDevice allocateDevice(final DeviceType deviceType) {
        List<CourgetteMobileDevice> devices = availableDevices
                .stream()
                .filter(device -> device.getDeviceType().equals(deviceType))
                .filter(device -> !unavailableDevices.contains(device))
                .collect(Collectors.toList());

        Collections.shuffle(devices);
        final CourgetteMobileDevice device = devices.get(0);
        unavailableDevices.add(device);
        return device;
    }

    public synchronized void deallocateDevice(final CourgetteMobileDevice device) {
        unavailableDevices.remove(device);
    }

    private List<CourgetteMobileDevice> createCourgetteMobileDevices(final String[] devices) {
        List<CourgetteMobileDevice> courgetteMobileDevices = new ArrayList<>();

        createDeviceSet(devices).forEach(device -> {
            List<String> deviceIdentifier = Arrays.stream(device.split(":")).map(String::trim).collect(Collectors.toList());

            String deviceName = deviceIdentifier.get(0);

            if (deviceIdentifier.size() > 1) {
                courgetteMobileDevices.add(new CourgetteMobileDevice(deviceName, DeviceType.REAL_DEVICE, deviceIdentifier.get(1), getParallelPort()));
            } else {
                courgetteMobileDevices.add(new CourgetteMobileDevice(deviceName, DeviceType.SIMULATOR, null, getParallelPort()));
            }
        });
        return courgetteMobileDevices;
    }

    private Set<String> createDeviceSet(final String[] devices) {
        Set<String> deviceSet = new HashSet<>();
        for (String device : devices) {
            if (deviceSet.stream().noneMatch(d -> d.toLowerCase().equals(device.toLowerCase().trim()))) {
                deviceSet.add(device.trim());
            }
        }
        return deviceSet;
    }

    private synchronized int getParallelPort() {
        int parallelPort = 0;
        try {
            ServerSocket socket = new ServerSocket(0);
            parallelPort = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            printError("Courgette Mobile Device Allocator: Unable to find a free port");
        }
        return parallelPort;
    }
}
