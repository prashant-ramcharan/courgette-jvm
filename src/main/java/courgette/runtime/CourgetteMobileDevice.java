package courgette.runtime;

public class CourgetteMobileDevice {
    private final String deviceName;
    private final DeviceType deviceType;
    private final String udid;
    private final int parallelPort;

    public CourgetteMobileDevice(String deviceName, DeviceType deviceType, String udid, int parallelPort) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.udid = udid;
        this.parallelPort = parallelPort;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getUdid() {
        return udid;
    }

    public int getParallelPort() {
        return parallelPort;
    }
}
