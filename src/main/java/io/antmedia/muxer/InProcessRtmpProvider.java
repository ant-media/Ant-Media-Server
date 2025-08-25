package io.antmedia.muxer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

import io.vertx.core.Vertx;
import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.IMessageComponent;


/**
 * Lightweight provider that converts encoded H.264 / AAC {@link AVPacket}s coming from
 * {@link io.antmedia.plugin.PacketFeeder} into Red5 RTMP messages and pushes them to an
 * {@link InMemoryPushPushPipe}.
    @Override
    public void writeTrailer(String streamId) {

    }

    @Override
    public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {

    }
T
    @Override
    public void writeTrailer(String streamId) {

    }

    @Override
    public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {

    }
h
    @Override
    public void writeTrailer(String streamId) {

    }

    @Override
    public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {

    }
e pipe is registered as a provider in a {@link org.red5.server.api.scope.IBroadcastScope},
 * so standard RTMP play ( {@code ProviderService.lookupProviderInput(...)=LIVE} ) works without opening a TCP socket.
 * NOTE – current implementation assumes
 *      • H.264 Annex-B video
 *      • AAC LC audio
 * and does not do any transcoding.  It wraps raw frames in minimum-viable FLV tags.
 * Further optimisations (SPS/PPS extraction, metadata, PTS/DTS re-ordering) can be added incrementally.
 */
public class InProcessRtmpProvider extends Muxer implements IProvider {


    private IBroadcastScope broadcastScope;
    private final AVRational videoTb;
    private final AVRational audioTb;

    private final AtomicInteger firstVideoTs = new AtomicInteger(-1);

    public InProcessRtmpProvider(IScope appScope, Vertx vertx, String streamId, AVRational videoTimebase, AVRational audioTimebase) {
        super(vertx);
        this.videoTb = videoTimebase;
        this.audioTb = audioTimebase;
        this.scope = appScope;
        this.streamId = streamId;
        this.broadcastScope = attachRtmpPublisher(streamId);
        this.isInitialized = true;
        allocateAVPacket();
    }
    public void setBroadcastScope(IBroadcastScope broadcastScope) {
        this.broadcastScope = broadcastScope;
    }

    public IBroadcastScope getBroadcastScope() {
        return broadcastScope;
    }

    @Override
    public synchronized boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) {
        if (codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO)
        {
            videoExtradata = new byte[codecParameters.extradata_size()];

            if(videoExtradata.length > 0) {
                BytePointer extraDataPointer = codecParameters.extradata();
                extraDataPointer.get(videoExtradata).close();
                extraDataPointer.close();
            }
            else
                videoExtradata = null;
        }
        super.addStream(codecParameters,timebase,streamIndex);
        return true;
    }

    @Override
    public synchronized void writePacket(AVPacket packet, AVRational inputTimebase, AVRational outputTimebase, int codecType){
        if (packet == null || packet.size() <= 0) {
            return;
        }
        try {
            if(codecType == AVMEDIA_TYPE_VIDEO) {
                int ts = (int) avutil.av_rescale_q(packet.pts(), videoTb, MuxAdaptor.TIME_BASE_FOR_MS);
                if (firstVideoTs.compareAndSet(-1, ts)) {
                    // remember first ts so we can normalise later if needed
                }
                
                if((packet.flags() & AV_PKT_FLAG_KEY)==1 && videoExtradata!=null){
                    super.addExtradataIfRequired(packet,true);
                    packet = tmpPacket;
                }

                ByteBuffer nioBuf = packet.data().limit(packet.size()).asByteBuffer();
                IoBuffer flvPayload = IoBuffer.allocate(5 + nioBuf.remaining());
                flvPayload.setAutoExpand(false);

                // FrameType (key/inter) + CodecID (7 = AVC)
                int frameType = ((packet.flags() & avcodec.AV_PKT_FLAG_KEY) != 0) ? 0x17 : 0x27; // 1:keyframe|7=AVC , 2:inter frame|7
                flvPayload.put((byte) frameType);
                flvPayload.put((byte) 0x01);             // AVC NALU packet
                flvPayload.put((byte) 0x00);             // composition time 0
                flvPayload.put((byte) 0x00);
                flvPayload.put((byte) 0x00);
                flvPayload.put(nioBuf);
                flvPayload.flip();

                VideoData videoData = new VideoData(flvPayload);
                videoData.setTimestamp(ts);
                RTMPMessage rtmp = RTMPMessage.build(videoData, Constants.SOURCE_TYPE_LIVE);
                broadcastScope.pushMessage(rtmp);
            } else if (codecType == AVMEDIA_TYPE_AUDIO) {
                int ts = (int) avutil.av_rescale_q(packet.pts(), audioTb, MuxAdaptor.TIME_BASE_FOR_MS);

                ByteBuffer nioBuf = packet.data().limit(packet.size()).asByteBuffer();
                IoBuffer flvPayload = IoBuffer.allocate(2 + nioBuf.remaining());
                flvPayload.setAutoExpand(false);
                // SoundFormat (10 = AAC)  | SoundRate 3=44k  | SoundSize 1=16-bit | SoundType 1=stereo
                flvPayload.put((byte) 0xAF);
                flvPayload.put((byte) 0x01); // AAC raw
                flvPayload.put(nioBuf);
                flvPayload.flip();

                AudioData audioData = new AudioData(flvPayload);
                audioData.setTimestamp(ts);
                RTMPMessage rtmp = RTMPMessage.build(audioData, Constants.SOURCE_TYPE_LIVE);
                broadcastScope.pushMessage(rtmp);
            }
        } catch (Exception e) {
            logger.error("Error while pushing video packet", e);
        }
    }

    public IBroadcastScope attachRtmpPublisher(String streamId) {
        // obtain or create broadcast scope
        IBroadcastScope bs = this.scope.getBroadcastScope(streamId);
        if (bs == null) {
            bs = new BroadcastScope(this.scope, streamId);
            this.scope.addChildScope(bs);
        }

        // connect provider to scope pipe
        bs.subscribe(this, null);

        logger.info("In-process RTMP pipeline ready for stream {}", streamId);
        return bs;
    }

    public void detachRtmpPublisher(String streamId) {
        IBroadcastScope bs = this.scope.getBroadcastScope(streamId);
        if (bs != null) {
            bs.unsubscribe(this);
        }
    }

    @Override
    public boolean isCodecSupported(int codecId) {
        return (codecId == AV_CODEC_ID_H264 || codecId == AV_CODEC_ID_AAC);
    }

    public AVFormatContext getOutputFormatContext() {
        if (outputFormatContext == null) {

            outputFormatContext= new AVFormatContext(null);
            int ret = avformat_alloc_output_context2(outputFormatContext, null, "null", null);

            if (ret < 0) {
                logger.info("Could not create output context for {}",  "");
                return null;
            }
        }
        return outputFormatContext;
    }

    @Override
    public boolean writeHeader() {
        setIsRunning(new AtomicBoolean(true));
        return true;
    }
    @Override
    public synchronized void writeTrailer() {
        detachRtmpPublisher(streamId);
    }
    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        // No special OOB handling required for internal publisher
    }
}
