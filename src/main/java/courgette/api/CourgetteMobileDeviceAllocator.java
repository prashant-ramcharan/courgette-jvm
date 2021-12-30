package courgette.api;

public final class CourgetteMobileDeviceAllocator {
    public static final String DEVICE_NAME = System.getProperty("courgette.mobile.device.name");
    public static final String UDID = System.getProperty("courgette.mobile.device.udid");
    public static final int PARALLEL_PORT = Integer.parseInt(System.getProperty("courgette.mobile.device.parallel.port"));
}
