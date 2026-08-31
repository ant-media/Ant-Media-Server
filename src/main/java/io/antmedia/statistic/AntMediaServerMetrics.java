package io.antmedia.statistic;

import java.lang.management.ManagementFactory;
import java.util.function.ToDoubleFunction;

import com.sun.management.OperatingSystemMXBean;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/** Exposes the Ant Media-specific instance statistics also published to Kafka. */
public class AntMediaServerMetrics implements MeterBinder {

    private static final String BYTES_BASE_UNIT = "bytes";
    private static final String PERCENT_BASE_UNIT = "percent";

    private final StatsCollector statsCollector;
    private final OperatingSystemMXBean operatingSystem;
    private final GPUUtils gpuUtils;

    public AntMediaServerMetrics(StatsCollector statsCollector) {
        this(statsCollector, (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean(),
                GPUUtils.getInstance());
    }

    AntMediaServerMetrics(StatsCollector statsCollector, OperatingSystemMXBean operatingSystem, GPUUtils gpuUtils) {
        this.statsCollector = statsCollector;
        this.operatingSystem = operatingSystem;
        this.gpuUtils = gpuUtils;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        gauge(registry, "antmedia.streams.live", "Number of local live streams",
                StatsCollector::getLocalLiveStreams);
        gauge(registry, "antmedia.streams.webrtc.live", "Number of local WebRTC live streams",
                StatsCollector::getLocalWebRTCLiveStreams);
        gauge(registry, "antmedia.viewers.webrtc", "Number of local WebRTC viewers",
                StatsCollector::getLocalWebRTCViewers);
        gauge(registry, "antmedia.viewers.hls", "Number of local HLS viewers",
                StatsCollector::getLocalHLSViewers);
        gauge(registry, "antmedia.viewers.dash", "Number of local DASH viewers",
                StatsCollector::getLocalDASHViewers);
        gauge(registry, "antmedia.encoders.blocked", "Number of blocked encoders",
                StatsCollector::getEncodersBlocked);
        gauge(registry, "antmedia.encoders.not.opened", "Number of encoders that could not be opened",
                StatsCollector::getEncodersNotOpened);
        gauge(registry, "antmedia.publish.timeout.errors", "Number of publish timeout errors",
                StatsCollector::getPublishTimeoutErrors);
        gauge(registry, "antmedia.db.query.average.duration.milliseconds",
                "Average datastore query duration in milliseconds", StatsCollector::getDBQueryAverageTimeMs);

        Gauge.builder("system.memory.total", operatingSystem,
                OperatingSystemMXBean::getTotalMemorySize)
                .description("Total physical memory available to the system")
                .baseUnit(BYTES_BASE_UNIT)
                .register(registry);
        Gauge.builder("system.memory.free", operatingSystem,
                OperatingSystemMXBean::getFreeMemorySize)
                .description("Free physical memory available to the system")
                .baseUnit(BYTES_BASE_UNIT)
                .register(registry);
        Gauge.builder("system.memory.used", operatingSystem,
                        system -> system.getTotalMemorySize() - system.getFreeMemorySize())
                .description("Used physical memory in the system")
                .baseUnit(BYTES_BASE_UNIT)
                .register(registry);

        Gauge.builder("antmedia.gpu.count", gpuUtils, GPUUtils::getDeviceCount)
                .description("Number of GPUs available to Ant Media Server")
                .register(registry);
        for (int deviceIndex = 0; deviceIndex < gpuUtils.getDeviceCount(); deviceIndex++) {
            registerGpuMetrics(registry, deviceIndex);
        }

        Gauge.builder("antmedia.vertx.worker.queue.size", statsCollector,
                        StatsCollector::getVertWorkerQueueSize)
                .description("Vert.x worker queue size")
                .tag("pool", "server")
                .register(registry);
        Gauge.builder("antmedia.vertx.worker.queue.size", statsCollector,
                        StatsCollector::getWebRTCVertxWorkerQueueSize)
                .description("Vert.x worker queue size")
                .tag("pool", "webrtc")
                .register(registry);
    }

    private void registerGpuMetrics(MeterRegistry registry, int deviceIndex) {
        String gpu = String.valueOf(deviceIndex);
        String name = gpuUtils.getDeviceName(deviceIndex);
        name = name == null ? "unknown" : name;

        Gauge.builder("antmedia.gpu.utilization", gpuUtils,
                        utils -> utils.getGPUUtilization(deviceIndex))
                .description("GPU utilization percentage")
                .baseUnit(PERCENT_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.memory.utilization", gpuUtils,
                        utils -> utils.getMemoryUtilization(deviceIndex))
                .description("GPU memory utilization percentage")
                .baseUnit(PERCENT_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.encoder.utilization", gpuUtils,
                        utils -> utils.getEncoderUtilization(deviceIndex))
                .description("GPU encoder utilization percentage")
                .baseUnit(PERCENT_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.decoder.utilization", gpuUtils,
                        utils -> utils.getDecoderUtilization(deviceIndex))
                .description("GPU decoder utilization percentage")
                .baseUnit(PERCENT_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.memory.total", gpuUtils,
                        utils -> getGpuMemory(utils, deviceIndex, MemoryValue.TOTAL))
                .description("Total GPU memory")
                .baseUnit(BYTES_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.memory.used", gpuUtils,
                        utils -> getGpuMemory(utils, deviceIndex, MemoryValue.USED))
                .description("Used GPU memory")
                .baseUnit(BYTES_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
        Gauge.builder("antmedia.gpu.memory.free", gpuUtils,
                        utils -> getGpuMemory(utils, deviceIndex, MemoryValue.FREE))
                .description("Free GPU memory")
                .baseUnit(BYTES_BASE_UNIT)
                .tags("gpu", gpu, "name", name)
                .register(registry);
    }

    private double getGpuMemory(GPUUtils utils, int deviceIndex, MemoryValue value) {
        GPUUtils.MemoryStatus status = utils.getMemoryStatus(deviceIndex);
        if (status == null) {
            return -1;
        }
        return switch (value) {
        case TOTAL -> status.getMemoryTotal();
        case USED -> status.getMemoryUsed();
        case FREE -> status.getMemoryFree();
        };
    }

    private enum MemoryValue {
        TOTAL, USED, FREE
    }

    private void gauge(MeterRegistry registry, String name, String description,
            ToDoubleFunction<StatsCollector> valueFunction) {
        Gauge.builder(name, statsCollector, valueFunction)
                .description(description)
                .register(registry);
    }
}
