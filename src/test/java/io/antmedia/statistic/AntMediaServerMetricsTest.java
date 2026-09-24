package io.antmedia.statistic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.management.OperatingSystemMXBean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("fast")
class AntMediaServerMetricsTest extends UnitTestBase<AntMediaServerMetrics> {

    private static final String GPU_NAME = "NVIDIA A10";
    private static final String BYTES_BASE_UNIT = "bytes";
    private static final String PERCENT_BASE_UNIT = "percent";

    @Test
    void shouldExposeInternalServerMetrics() {
        StatsCollector statsCollector = mock(StatsCollector.class);
        when(statsCollector.getLocalLiveStreams()).thenReturn(3);
        when(statsCollector.getLocalWebRTCViewers()).thenReturn(7);
        when(statsCollector.getVertWorkerQueueSize()).thenReturn(2);
        when(statsCollector.getWebRTCVertxWorkerQueueSize()).thenReturn(4);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new AntMediaServerMetrics(statsCollector).bindTo(registry);

        assertThat(registry.get("antmedia.streams.live").gauge().value()).isEqualTo(3);
        assertThat(registry.get("antmedia.viewers.webrtc").gauge().value()).isEqualTo(7);
        assertThat(registry.get("antmedia.vertx.worker.queue.size").tag("pool", "server")
                .gauge().value()).isEqualTo(2);
        assertThat(registry.get("antmedia.vertx.worker.queue.size").tag("pool", "webrtc")
                .gauge().value()).isEqualTo(4);
        assertThat(registry.get("system.memory.total").gauge().value()).isGreaterThan(0);
        assertThat(registry.get("system.memory.free").gauge().value()).isGreaterThanOrEqualTo(0);
        assertThat(registry.get("system.memory.used").gauge().value()).isGreaterThanOrEqualTo(0);
        assertThat(registry.get("antmedia.gpu.count").gauge().value()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldExposeGpuMetrics() {
        StatsCollector statsCollector = mock(StatsCollector.class);
        OperatingSystemMXBean operatingSystem = mock(OperatingSystemMXBean.class);
        GPUUtils gpuUtils = mock(GPUUtils.class);
        when(gpuUtils.getDeviceCount()).thenReturn(2);
        when(gpuUtils.getDeviceName(0)).thenReturn(GPU_NAME);
        when(gpuUtils.getDeviceName(1)).thenReturn(null);
        when(gpuUtils.getGPUUtilization(0)).thenReturn(41);
        when(gpuUtils.getMemoryUtilization(0)).thenReturn(52);
        when(gpuUtils.getEncoderUtilization(0)).thenReturn(63);
        when(gpuUtils.getDecoderUtilization(0)).thenReturn(74);
        when(gpuUtils.getMemoryStatus(0)).thenReturn(new GPUUtils.MemoryStatus(1_000, 400, 600));
        when(gpuUtils.getMemoryStatus(1)).thenReturn(null);

        classUnderTest = new AntMediaServerMetrics(statsCollector, operatingSystem, gpuUtils);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        classUnderTest.bindTo(registry);

        assertThat(registry.get("antmedia.gpu.count").gauge().value()).isEqualTo(2);
        assertGpuGauge(registry, "antmedia.gpu.utilization", "0", GPU_NAME, 41, PERCENT_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.memory.utilization", "0", GPU_NAME, 52, PERCENT_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.encoder.utilization", "0", GPU_NAME, 63, PERCENT_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.decoder.utilization", "0", GPU_NAME, 74, PERCENT_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.memory.total", "0", GPU_NAME, 1_000, BYTES_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.memory.used", "0", GPU_NAME, 400, BYTES_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.memory.free", "0", GPU_NAME, 600, BYTES_BASE_UNIT);
        assertGpuGauge(registry, "antmedia.gpu.memory.total", "1", "unknown", -1, BYTES_BASE_UNIT);
    }

    private void assertGpuGauge(SimpleMeterRegistry registry, String metricName, String gpu, String name,
            double expectedValue, String baseUnit) {
        Gauge gauge = registry.get(metricName).tags("gpu", gpu, "name", name).gauge();
        assertThat(gauge.value()).isEqualTo(expectedValue);
        assertThat(gauge.getId().getBaseUnit()).isEqualTo(baseUnit);
    }
}
