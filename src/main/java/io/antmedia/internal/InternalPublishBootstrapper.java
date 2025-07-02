package io.antmedia.internal;

import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.BroadcastScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.PacketFeeder;

/**
 * Utility to bootstrap an in-process live stream for a WebRTC publisher.
 * It creates (or reuses) a {@link BroadcastScope}, wires an {@link InProcessRtmpPublisher}
 * to its internal pipe, and registers the scope via {@link IProviderService} so that
 * downstream consumers (RTMP/HLS/PlayEngine) view it as a normal LIVE provider.
 */
public class InternalPublishBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(InternalPublishBootstrapper.class);

    public static InProcessRtmpPublisher attach(IScope appScope,
                                                String streamId,
                                                PacketFeeder feeder,
                                                MuxAdaptor muxAdaptor) {
        // obtain or create broadcast scope
        IBroadcastScope bs = appScope.getBroadcastScope(streamId);
        if (bs == null) {
            bs = new BroadcastScope(appScope, streamId);
            appScope.addChildScope(bs);
        }

        // Create publisher instance bound to this scope
        InProcessRtmpPublisher publisher = new InProcessRtmpPublisher(
                (BroadcastScope) bs,
                muxAdaptor.getVideoTimeBase(),
                muxAdaptor.getAudioTimeBase());

        // connect provider to scope pipe
        bs.subscribe(publisher, null);

        // listen to encoded packets coming from WebRTC side
        feeder.addListener(publisher);

        log.info("In-process RTMP pipeline ready for stream {}", streamId);
        return publisher;
    }

    public static void detach(IScope appScope,
                               String streamId,
                               PacketFeeder feeder,
                               InProcessRtmpPublisher publisher) {
        if (publisher == null) {
            return;
        }
        IBroadcastScope bs = appScope.getBroadcastScope(streamId);
        if (bs != null) {
            bs.unsubscribe(publisher);
        }
        feeder.removeListener(publisher);
    }
} 