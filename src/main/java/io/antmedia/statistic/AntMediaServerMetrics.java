package io.antmedia.statistic;

import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/** Exposes the Ant Media-specific instance statistics also published to Kafka. */
public class AntMediaServerMetrics implements MeterBinder {

    private final StatsCollector statsCollector;

    public AntMediaServerMetrics(StatsCollector statsCollector) {
        this.statsCollector = statsCollector;
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

    private void gauge(MeterRegistry registry, String name, String description,
            ToDoubleFunction<StatsCollector> valueFunction) {
        Gauge.builder(name, statsCollector, valueFunction)
                .description(description)
                .register(registry);
    }
}
