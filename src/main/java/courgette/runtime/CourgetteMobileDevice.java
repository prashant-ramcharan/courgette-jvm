package courgette.runtime;

public class CourgetteMobileDevice {
    private final String deviceName;
    private final String udid;
    private final int parallelPort;

    public CourgetteMobileDevice(String deviceName, String udid, int parallelPort) {
        this.deviceName = deviceName;
        this.udid = udid;
        this.parallelPort = parallelPort;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUdid() {
        return udid;
    }

    public int getParallelPort() {
        return parallelPort;
    }
}
