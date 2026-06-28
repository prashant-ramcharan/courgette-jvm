package courgette.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourgetteMobileDeviceAllocatorServiceTest {
    @Test
    public void allocatesSimulatorAndRealDevices() {
        CourgetteMobileDeviceAllocatorService allocator = new CourgetteMobileDeviceAllocatorService(
                new String[]{"iPhone", "iPhone", "Pixel:abc"});

        CourgetteMobileDevice simulator = allocator.allocateDevice(DeviceType.SIMULATOR);
        CourgetteMobileDevice realDevice = allocator.allocateDevice(DeviceType.REAL_DEVICE);

        assertEquals("iPhone", simulator.getDeviceName());
        assertEquals(DeviceType.SIMULATOR, simulator.getDeviceType());
        assertNull(simulator.getUdid());
        assertEquals("Pixel", realDevice.getDeviceName());
        assertEquals(DeviceType.REAL_DEVICE, realDevice.getDeviceType());
        assertEquals("abc", realDevice.getUdid());
        assertTrue(simulator.getParallelPort() > 0);
        assertTrue(realDevice.getParallelPort() > 0);
        assertNotEquals(simulator.getParallelPort(), realDevice.getParallelPort());
    }

    @Test
    public void deallocatedDevicesCanBeAllocatedAgain() {
        CourgetteMobileDeviceAllocatorService allocator = new CourgetteMobileDeviceAllocatorService(new String[]{"iPhone"});

        CourgetteMobileDevice device = allocator.allocateDevice(DeviceType.SIMULATOR);
        allocator.deallocateDevice(device);

        assertNotNull(allocator.allocateDevice(DeviceType.SIMULATOR));
    }
}
