package io.antmedia.statistic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.test.UnitTestBase;
import io.antmedia.webrtc.api.IWebRTCAdaptor;

@Tag("fast")
class StatsCollectorUnitTest extends UnitTestBase<StatsCollector> {

    @Test
    void shouldAggregateLocalServerMetricsAcrossScopes() {
        AntMediaApplicationAdapter adaptor = mock(AntMediaApplicationAdapter.class);
        when(adaptor.getMuxAdaptors()).thenReturn(List.of(mock(MuxAdaptor.class), mock(MuxAdaptor.class)));
        when(adaptor.getNumberOfEncodersBlocked()).thenReturn(3);
        when(adaptor.getNumberOfEncoderNotOpenedErrors()).thenReturn(4);
        when(adaptor.getNumberOfPublishTimeoutError()).thenReturn(5);

        IWebRTCAdaptor webRTCAdaptor = mock(IWebRTCAdaptor.class);
        when(webRTCAdaptor.getNumberOfLiveStreams()).thenReturn(6);
        when(webRTCAdaptor.getNumberOfTotalViewers()).thenReturn(7);
        when(webRTCAdaptor.getNumberOfTotalViewersExcludingEdges()).thenReturn(6);

        HlsViewerStats hlsViewerStats = mock(HlsViewerStats.class);
        when(hlsViewerStats.getTotalViewerCount()).thenReturn(8);
        when(hlsViewerStats.getTotalViewerCountExcludingEdges()).thenReturn(7);
        DashViewerStats dashViewerStats = mock(DashViewerStats.class);
        when(dashViewerStats.getTotalViewerCount()).thenReturn(9);
        when(dashViewerStats.getTotalViewerCountExcludingEdges()).thenReturn(8);

        ApplicationContext populatedContext = mock(ApplicationContext.class);
        when(populatedContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(true);
        when(populatedContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(adaptor);
        when(populatedContext.containsBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(true);
        when(populatedContext.getBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(webRTCAdaptor);
        when(populatedContext.containsBean(HlsViewerStats.BEAN_NAME)).thenReturn(true);
        when(populatedContext.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(hlsViewerStats);
        when(populatedContext.containsBean(DashViewerStats.BEAN_NAME)).thenReturn(true);
        when(populatedContext.getBean(DashViewerStats.BEAN_NAME)).thenReturn(dashViewerStats);

        ApplicationContext emptyContext = mock(ApplicationContext.class);
        Queue<IScope> scopes = new ConcurrentLinkedQueue<>();
        scopes.add(scopeWith(populatedContext));
        scopes.add(scopeWith(emptyContext));

        StatsCollector statsCollector = new StatsCollector();
        statsCollector.setScopes(scopes);

        assertThat(statsCollector.getLocalLiveStreams()).isEqualTo(2);
        assertThat(statsCollector.getLocalWebRTCLiveStreams()).isEqualTo(6);
        assertThat(statsCollector.getLocalWebRTCViewers()).isEqualTo(7);
        assertThat(statsCollector.getLocalHLSViewers()).isEqualTo(8);
        assertThat(statsCollector.getLocalDASHViewers()).isEqualTo(9);
        assertThat(statsCollector.getLocalViewers()).isEqualTo(6 + 7 + 8);
        assertThat(statsCollector.getEncodersBlocked()).isEqualTo(3);
        assertThat(statsCollector.getEncodersNotOpened()).isEqualTo(4);
        assertThat(statsCollector.getPublishTimeoutErrors()).isEqualTo(5);
    }

    @Test
    void shouldReportAverageGPUUtilizationAndModel() {
        GPUUtils gpuUtils = mock(GPUUtils.class);
        when(gpuUtils.getDeviceCount()).thenReturn(2);
        when(gpuUtils.getGPUUtilization(0)).thenReturn(40);
        when(gpuUtils.getGPUUtilization(1)).thenReturn(61);
        when(gpuUtils.getDeviceName(0)).thenReturn("NVIDIA T4");
        when(gpuUtils.getDeviceName(1)).thenReturn("NVIDIA T4");

        StatsCollector statsCollector = new StatsCollector();
        try (MockedStatic<GPUUtils> gpuUtilsStatic = mockStatic(GPUUtils.class)) {
            gpuUtilsStatic.when(GPUUtils::getInstance).thenReturn(gpuUtils);
            assertThat(statsCollector.getLocalGPUUtilization()).isEqualTo(51);
            assertThat(statsCollector.getLocalGPUModel()).isEqualTo("2x NVIDIA T4");

            when(gpuUtils.getDeviceName(1)).thenReturn("NVIDIA A10");
            when(gpuUtils.getGPUUtilization(1)).thenReturn(-1);
            assertThat(statsCollector.getLocalGPUUtilization()).isEqualTo(40);
            assertThat(statsCollector.getLocalGPUModel()).isEqualTo("NVIDIA T4, NVIDIA A10");

            when(gpuUtils.getDeviceCount()).thenReturn(0);
            assertThat(statsCollector.getLocalGPUUtilization()).isNull();
            assertThat(statsCollector.getLocalGPUModel()).isNull();
        }
    }

    private IScope scopeWith(ApplicationContext applicationContext) {
        IContext context = mock(IContext.class);
        when(context.getApplicationContext()).thenReturn(applicationContext);
        IScope scope = mock(IScope.class);
        when(scope.getContext()).thenReturn(context);
        return scope;
    }
}
