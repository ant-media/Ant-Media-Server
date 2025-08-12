package io.antmedia.streamsource;
import io.vertx.core.Vertx;
import io.antmedia.filter.JWTFilter;
import io.antmedia.rtmp.InProcessRtmpPublisher;
import org.red5.server.api.scope.IScope;

public class InternalStreamFetcher extends StreamFetcher {

    public String rtmpUrl;

    public InternalStreamFetcher(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx, long seekTimeInMs) {
        super(streamUrl, streamId, streamType, scope, vertx, seekTimeInMs);
        this.rtmpUrl = streamUrl;
        super.setStreamUrl(this.getStreamUrl());
    }
    public InProcessRtmpPublisher inProcessRtmpPublisher;

    public void setInProcessRtmpPublisher(InProcessRtmpPublisher inProcessRtmpPublisher) {
      this.inProcessRtmpPublisher = inProcessRtmpPublisher;
    }

    public InProcessRtmpPublisher getInProcessRtmpPublisher() {
      return inProcessRtmpPublisher;
    }

    @Override
    public String getStreamUrl(){
        String jwtToken = JWTFilter.generateJwtToken(
        getAppSettings().getClusterCommunicationKey(),
        System.currentTimeMillis() + 30000);
        String tokenUrl = this.rtmpUrl + "?token=" + jwtToken;

        return tokenUrl;
    }


}
