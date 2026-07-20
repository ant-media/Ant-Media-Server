package io.antmedia.statistic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.test.UnitTestBase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("fast")
class AntMediaServerMetricsTest extends UnitTestBase<AntMediaServerMetrics> {

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
    }
}
