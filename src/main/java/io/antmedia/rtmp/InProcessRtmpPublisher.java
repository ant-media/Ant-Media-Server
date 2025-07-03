package io.antmedia.rtmp;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.StreamParametersInfo;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.IMessageComponent;

/**
 * Lightweight provider that converts encoded H.264 / AAC {@link AVPacket}s coming from
 * {@link io.antmedia.plugin.PacketFeeder} into Red5 RTMP messages and pushes them to an
 * {@link InMemoryPushPushPipe}.  The pipe is registered as a provider in a {@link org.red5.server.api.scope.IBroadcastScope},
 * so standard RTMP play ( {@code ProviderService.lookupProviderInput(...)=LIVE} ) works without opening a TCP socket.
 * <p>
 * NOTE – current implementation assumes<br/>
 *      • H.264 Annex-B video<br/>
 *      • AAC LC audio<br/>
 * and <b>does not</b> do any transcoding.  It wraps raw frames in minimum-viable FLV tags.
 * Further optimisations (SPS/PPS extraction, metadata, PTS/DTS re-ordering) can be added incrementally.
 */
public class InProcessRtmpPublisher implements IPacketListener, IProvider {

    private static final Logger logger = LoggerFactory.getLogger(InProcessRtmpPublisher.class);

    private final BroadcastScope scope;
    private final AVRational videoTb;
    private final AVRational audioTb;

    private final AtomicInteger firstVideoTs = new AtomicInteger(-1);

    private volatile boolean videoSeqHdrSent = false;
    private volatile boolean audioSeqHdrSent = false;

    public InProcessRtmpPublisher(BroadcastScope scope, AVRational videoTimebase, AVRational audioTimebase) {
        this.scope = scope;
        this.videoTb = videoTimebase;
        this.audioTb = audioTimebase;
    }

    // --------------------------------------------------------------------
    //  IPacketListener implementation
    // --------------------------------------------------------------------

    @Override
    public AVPacket onVideoPacket(String streamId, AVPacket packet) {
        if (packet == null || packet.size() <= 0) {
            return packet;
        }
        try {
            int ts = (int) avutil.av_rescale_q(packet.pts(), videoTb, MuxAdaptor.TIME_BASE_FOR_MS);
            if (firstVideoTs.compareAndSet(-1, ts)) {
                // remember first ts so we can normalise later if needed
            }

            ByteBuffer nioBuf = packet.data().limit(packet.size()).asByteBuffer();
            IoBuffer flvPayload = IoBuffer.allocate(5 + nioBuf.remaining());
            flvPayload.setAutoExpand(false);

            // FrameType (key/inter) + CodecID (7 = AVC)
            int frameType = (packet.flags() & avcodec.AV_PKT_FLAG_KEY) != 0 ? 0x10 : 0x20; // KEY=1(interleaved) but shift later
            frameType = ((packet.flags() & avcodec.AV_PKT_FLAG_KEY) != 0) ? 0x17 : 0x27; // 1:keyframe|7=AVC , 2:inter frame|7
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
            scope.pushMessage(rtmp);
        } catch (Exception e) {
            logger.error("Error while pushing video packet", e);
        }
        return packet;
    }

    @Override
    public AVPacket onAudioPacket(String streamId, AVPacket packet) {
        if (packet == null || packet.size() <= 0) {
            return packet;
        }
        try {
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
            scope.pushMessage(rtmp);
        } catch (Exception e) {
            logger.error("Error while pushing audio packet", e);
        }
        return packet;
    }

    @Override
    public void writeTrailer(String streamId) {
        // send onStatus or simply ignore – RTMP connection will detect EOS when pipe unsubscribes
    }

    // Metadata not used for now
    @Override public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {}
    @Override public void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo) {}

    // --------------------------------------------------------------------
    // IProvider / IMessageComponent dummy implementation
    // --------------------------------------------------------------------
    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        // No special OOB handling required for internal publisher
    }
} 