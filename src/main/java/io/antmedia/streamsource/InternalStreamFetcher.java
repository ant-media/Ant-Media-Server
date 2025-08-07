package io.antmedia.streamsource;
import io.antmedia.streamsource.StreamFetcher;
import io.vertx.core.Vertx;
import io.antmedia.rtmp.InProcessRtmpPublisher;
import org.red5.server.api.scope.IScope;

public class InternalStreamFetcher extends StreamFetcher {


    public InternalStreamFetcher(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx, long seekTimeInMs) {
        super(streamUrl, streamId, streamType, scope, vertx, seekTimeInMs);
    }
    public InProcessRtmpPublisher inProcessRtmpPublisher;

    public void setInProcessRtmpPublisher(InProcessRtmpPublisher inProcessRtmpPublisher) {
      this.inProcessRtmpPublisher = inProcessRtmpPublisher;
    }

    public InProcessRtmpPublisher getInProcessRtmpPublisher() {
      return inProcessRtmpPublisher;
    }


}
