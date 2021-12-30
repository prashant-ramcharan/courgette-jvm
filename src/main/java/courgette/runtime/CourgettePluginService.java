package courgette.runtime;

public class CourgettePluginService {

    private final CourgetteMobileDeviceAllocatorService courgetteMobileDeviceAllocatorService;

    public CourgettePluginService(CourgetteMobileDeviceAllocatorService courgetteMobileDeviceAllocatorService) {
        this.courgetteMobileDeviceAllocatorService = courgetteMobileDeviceAllocatorService;
    }

    public CourgetteMobileDeviceAllocatorService getCourgetteMobileDeviceAllocatorService() {
        return courgetteMobileDeviceAllocatorService;
    }
}
