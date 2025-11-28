package io.antmedia.muxer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_new_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.Vertx;

public class RtmpMuxerFlowTest {

    private TestableRtmpMuxer muxer;
    private Vertx vertx;

    @BeforeClass
    public static void beforeClass() {
        try {
            avformat.avformat_network_init();
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native libraries not found.");
        }
    }

    @Before
    public void setUp() {
        vertx = mock(Vertx.class);
        muxer = new TestableRtmpMuxer("rtmp://test.com/live/stream", vertx);
    }

    @After
    public void tearDown() {
        if (muxer != null) {
             muxer.clearResource();
        }
    }

    @Test
    public void testNormalPacketFlow() {
        // Initialize muxer (header written -> allows queuing)
        muxer.writeHeader();

        // 1. Send Video Packet (P-Frame)
        AVPacket videoPkt = createPacket(0, false); // Stream 0, Not Keyframe
        muxer.writePacket(videoPkt, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        assertEquals("Queue should have 1 packet", 1, muxer.getQueueSize());

        // 2. Send Audio Packet
        AVPacket audioPkt = createPacket(1, false); // Stream 1
        muxer.writePacket(audioPkt, muxer.getTimeBase(1), muxer.getTimeBase(1), avutil.AVMEDIA_TYPE_AUDIO);

        assertEquals("Queue should have 2 packets", 2, muxer.getQueueSize());
        
        av_packet_free(videoPkt);
        av_packet_free(audioPkt);
    }

    @Test
    public void testCongestionTailDrop() {
        muxer.writeHeader();

        // 1. Fill queue to capacity 
        for (int i = 0; i < RtmpMuxer.QUEUE_CAPACITY; i++) {
            AVPacket pkt = createPacket(1, false); 
            muxer.writePacket(pkt, muxer.getTimeBase(1), muxer.getTimeBase(1), avutil.AVMEDIA_TYPE_AUDIO);
            av_packet_free(pkt); // writePacket copies it, so we free our source
        }
        assertEquals(RtmpMuxer.QUEUE_CAPACITY, muxer.getQueueSize());
        assertFalse(muxer.isDroppingPframes());

        // 2. Try to add Video P-Frame (Expendable)
        AVPacket pFrame = createPacket(0, false);
        muxer.writePacket(pFrame, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        // Expect: Dropped, Size remains RtmpMuxer., Flag set
        assertEquals(RtmpMuxer.QUEUE_CAPACITY, muxer.getQueueSize());
        assertTrue("Should start dropping P-frames", muxer.isDroppingPframes());
        
        av_packet_free(pFrame);
    }

    @Test
    public void testAtomicGOPRecovery() {
        muxer.writeHeader();
        // Force into dropping state
        muxer.setDroppingPframes(true);

        // 1. Send P-Frame -> Should be ignored
        AVPacket pFrame = createPacket(0, false);
        muxer.writePacket(pFrame, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);
        assertEquals(0, muxer.getQueueSize());

        // 2. Send Keyframe -> Should be accepted and reset state
        AVPacket keyFrame = createPacket(0, true);
        muxer.writePacket(keyFrame, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);
        
        assertEquals(1, muxer.getQueueSize());
        assertFalse("Should stop dropping P-frames after Keyframe", muxer.isDroppingPframes());
        
        av_packet_free(pFrame);
        av_packet_free(keyFrame);
    }

    @Test
    public void testSmartSeek_HeadDrop() {
        muxer.writeHeader();

        // Scenario: Queue full with P-frames/Audio at head.
        // Insert Keyframe -> Should drop head to make room.
        
        // Fill with Audio (VIP but expendable for Keyframe insert)
        for (int i = 0; i < RtmpMuxer.QUEUE_CAPACITY; i++) {
            AVPacket pkt = createPacket(1, false);
            muxer.writePacket(pkt, muxer.getTimeBase(1), muxer.getTimeBase(1), avutil.AVMEDIA_TYPE_AUDIO);
            av_packet_free(pkt);
        }
        assertEquals(RtmpMuxer.QUEUE_CAPACITY, muxer.getQueueSize());

        // Force Insert Keyframe
        AVPacket keyFrame = createPacket(0, true);
        muxer.writePacket(keyFrame, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        assertEquals("Queue should stay full (one out, one in)", RtmpMuxer.QUEUE_CAPACITY, muxer.getQueueSize());
        
        av_packet_free(keyFrame);
    }

    @Test
    public void testSmartSeek_DropGop_WhenHeadIsKeyframe() {
        muxer.writeHeader();

        // 1. Setup Queue Structure: [Keyframe A (PTS 0), P-Frame B (PTS 10), Keyframe C (PTS 20), ...Audio...]
        
        // Packet A: Keyframe (Head)
        AVPacket pktA = createPacket(0, true);
        pktA.pts(0);
        muxer.writePacket(pktA, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        // Packet B: P-Frame (Should be dropped along with Head)
        AVPacket pktB = createPacket(0, false);
        pktB.pts(10);
        muxer.writePacket(pktB, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        // Packet C: Next Keyframe (Should become the NEW Head)
        AVPacket pktC = createPacket(0, true);
        pktC.pts(20);
        muxer.writePacket(pktC, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        // Fill remainder with Audio to reach capacity
        int filledCount = 3;
        for (int i = filledCount; i < RtmpMuxer.QUEUE_CAPACITY; i++) {
            AVPacket pad = createPacket(1, false);
            muxer.writePacket(pad, muxer.getTimeBase(1), muxer.getTimeBase(1), avutil.AVMEDIA_TYPE_AUDIO);
            av_packet_free(pad);
        }
        
        assertEquals("Setup: Queue must be full", RtmpMuxer.QUEUE_CAPACITY, muxer.getQueueSize());

        // 2. Trigger Smart Seek by inserting a new VIP Keyframe
        AVPacket newKey = createPacket(0, true);
        newKey.pts(5000);
        muxer.writePacket(newKey, muxer.getTimeBase(0), muxer.getTimeBase(0), avutil.AVMEDIA_TYPE_VIDEO);

        // 3. Verify Logic
        // Queue size logic: We dropped 2 packets (A & B), and added 1 (newKey).
        // Expected Size = Capacity - 1
        assertEquals("Queue size check", RtmpMuxer.QUEUE_CAPACITY - 1, muxer.getQueueSize());

        // The most important check: Did we stop dropping at Keyframe C?
        AVPacket newHead = muxer.peekPacket();
        assertTrue("Queue should not be empty", newHead != null);
        assertEquals("Head should be Keyframe C (PTS 20)", 20, newHead.pts());
        
        // Cleanup
        av_packet_free(pktA);
        av_packet_free(pktB);
        av_packet_free(pktC);
        av_packet_free(newKey);
    }

    // Helper
    private AVPacket createPacket(int streamIndex, boolean isKeyFrame) {
        // Use Native Allocator to ensure compatibility with av_packet_ref
        AVPacket pkt = av_packet_alloc();
        av_new_packet(pkt, 100); // Allocate 100 bytes data
        pkt.stream_index(streamIndex);
        if (isKeyFrame) {
            pkt.flags(avcodec.AV_PKT_FLAG_KEY);
        }
        pkt.pts(0);
        pkt.dts(0);
        pkt.duration(100);
        return pkt;
    }

    // --- Subclass with additional testing stuff used in Testing
    public static class TestableRtmpMuxer extends RtmpMuxer {
        
        public TestableRtmpMuxer(String url, Vertx vertx) {
            super(url, vertx);
            
            // CRITICAL FIX: Explicitly allocate valid native packet for 'tmpPacket'
            // This prevents SIGSEGV in writeFrameInternal -> av_packet_ref
            this.tmpPacket = av_packet_alloc();

            // Setup real AVFormatContext
            this.outputFormatContext = new AVFormatContext(null);
            avformat.avformat_alloc_output_context2(this.outputFormatContext, null, "flv", null);
            
            // Setup Video Stream (0)
            AVStream videoStream = avformat.avformat_new_stream(this.outputFormatContext, null);
            videoStream.id(0);
            videoStream.codecpar().codec_type(avutil.AVMEDIA_TYPE_VIDEO);
            videoStream.codecpar().codec_id(avcodec.AV_CODEC_ID_H264);
            
            // Setup Audio Stream (1)
            AVStream audioStream = avformat.avformat_new_stream(this.outputFormatContext, null);
            audioStream.id(1);
            audioStream.codecpar().codec_type(avutil.AVMEDIA_TYPE_AUDIO);
            audioStream.codecpar().codec_id(avcodec.AV_CODEC_ID_AAC);

            // Manual Map Setup (Simulating addStream)
            this.inputOutputStreamIndexMap.put(0, 0);
            this.inputOutputStreamIndexMap.put(1, 1);

            AVRational timebase = new AVRational();
            timebase.num(1);
            timebase.den(1000);
            this.inputTimeBaseMap.put(0, timebase);
            this.inputTimeBaseMap.put(1, timebase);
        }

        @Override
        public void startWorkerThread() {
            // HACK: Do NOT start thread. Just set flag so we can enqueue.
            this.isWorkerRunning = true;
        }

        @Override
        public boolean writeSuperHeader() {
            // Skip actual FFmpeg header write to avoid IO
            return true;
        }
        
        // Accessors for verification
        public int getQueueSize() {
            return this.packetQueue.size();
        }
        
        public boolean isDroppingPframes() {
            return this.droppingPframes;
        }

        public void setDroppingPframes(boolean v) {
            this.droppingPframes = v;
        }
        
        public AVRational getTimeBase(int streamIndex) {
            return this.inputTimeBaseMap.get(streamIndex);
        }

        public AVPacket peekPacket() {
            Object o = this.packetQueue.peek();
            if (o instanceof AVPacket) return (AVPacket) o;
            return null;
        }
        
        @Override
        public synchronized void clearResource() {
             if (tmpPacket != null) {
                 av_packet_free(tmpPacket);
                 tmpPacket = null;
             }
             super.clearResource();
        }
    }
}
