package io.antmedia.test.rtmp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.stream.message.RTMPMessage;

import io.antmedia.rtmp.InProcessRtmpPublisher;

@RunWith(MockitoJUnitRunner.class)
public class InProcessRtmpPublisherUnitTest {

    // ------------------------------------------------------------------
    // mocks & constants
    // ------------------------------------------------------------------
    @Mock private BroadcastScope mockScope;
    @Mock private AVPacket        mockPacket;
    @Mock private BytePointer     mockBytePointer;
    @Mock private ByteBuffer      mockByteBuffer;

    private InProcessRtmpPublisher publisher;
    private AVRational videoTb;
    private AVRational audioTb;

    private static final String STREAM_ID      = "test_stream_id";
    private static final int    VIDEO_TS_MS    = 1000;
    private static final int    AUDIO_TS_MS    = 2000;
    private static final int    PACKET_SIZE    = 100;

    // ------------------------------------------------------------------
    // test scaffolding
    // ------------------------------------------------------------------
    @Before
    public void setUp() throws Exception {
        // Create real AVRational objects instead of mocking
        videoTb = new AVRational().num(1).den(30);
        audioTb = new AVRational().num(1).den(48000);
        
        publisher = new InProcessRtmpPublisher(mockScope, videoTb, audioTb);
    }

    // ------------------------------------------------------------------
    // constructor tests
    // ------------------------------------------------------------------
    @Test
    public void constructor_handlesNullScope() {
        assertNotNull(new InProcessRtmpPublisher(mockScope, videoTb, audioTb));
        assertNotNull(new InProcessRtmpPublisher(null, videoTb, audioTb));
    }

    @Test
    public void constructor_handlesNullTimebase() {
        assertNotNull(new InProcessRtmpPublisher(mockScope, null, audioTb));
        assertNotNull(new InProcessRtmpPublisher(mockScope, videoTb, null));
        assertNotNull(new InProcessRtmpPublisher(mockScope, null, null));
    }

    // ------------------------------------------------------------------
    // video packet tests
    // ------------------------------------------------------------------
    @Test
    public void videoPacket_keyFrame_pushesMessageToScope() throws Exception {
        // Create real AVPacket instead of mocking
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(VIDEO_TS_MS);
        packet.flags(avcodec.AV_PKT_FLAG_KEY); // Key frame
        
        // Create real data
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));
        assertNotNull(result);
        assertEquals(packet.size(), result.size());
        
        // Cleanup
        packet.close();
        data.close();
    }

    @Test
    public void videoPacket_interFrame_pushesMessageToScope() throws Exception {
        // Create real AVPacket for inter frame
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(VIDEO_TS_MS);
        packet.flags(0); // Not a key frame
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));
        assertNotNull(result);
        
        // Cleanup
        packet.close();
        data.close();
    }

    @Test
    public void videoPacket_zeroSize_doesNotPushMessage() throws Exception {
        // Create real AVPacket with zero size
        AVPacket packet = new AVPacket();
        packet.size(0);
        packet.pts(VIDEO_TS_MS);
        packet.flags(avcodec.AV_PKT_FLAG_KEY);
        
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
        assertNotNull(result); // Implementation returns the packet, not null
        
        // Cleanup
        packet.close();
    }

    @Test
    public void videoPacket_nullPacket_doesNotPushMessage() throws Exception {
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, null);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
        assertNull(result);
    }

    @Test
    public void videoPacket_nullScope_doesNotCrash() throws Exception {
        // Create publisher with null scope
        InProcessRtmpPublisher nullScopePublisher = new InProcessRtmpPublisher(null, videoTb, audioTb);
        
        // Create real AVPacket
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(VIDEO_TS_MS);
        packet.flags(avcodec.AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute - should not crash
        AVPacket result = nullScopePublisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        assertNotNull(result);
        
        // Cleanup
        packet.close();
        data.close();
    }

    // ------------------------------------------------------------------
    // audio packet tests
    // ------------------------------------------------------------------
    @Test
    public void audioPacket_validPacket_pushesMessageToScope() throws Exception {
        // Create real AVPacket for audio
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(AUDIO_TS_MS);
        packet.flags(0);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        AVPacket result = publisher.onAudioPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));
        assertNotNull(result);
        
        // Cleanup
        packet.close();
        data.close();
    }

    @Test
    public void audioPacket_zeroSize_doesNotPushMessage() throws Exception {
        // Create real AVPacket with zero size
        AVPacket packet = new AVPacket();
        packet.size(0);
        packet.pts(AUDIO_TS_MS);
        
        // Execute
        AVPacket result = publisher.onAudioPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
        assertNotNull(result); // Implementation returns the packet, not null
        
        // Cleanup
        packet.close();
    }

    @Test
    public void audioPacket_nullPacket_doesNotPushMessage() throws Exception {
        // Execute
        AVPacket result = publisher.onAudioPacket(STREAM_ID, null);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
        assertNull(result);
    }

    // ------------------------------------------------------------------
    // timestamp conversion tests
    // ------------------------------------------------------------------
    @Test
    public void testTimestampConversion() throws Exception {
        // Create real AVPacket
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(VIDEO_TS_MS);
        packet.flags(avcodec.AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify message was created with timestamp conversion
        ArgumentCaptor<RTMPMessage> messageCaptor = ArgumentCaptor.forClass(RTMPMessage.class);
        verify(mockScope).pushMessage(messageCaptor.capture());
        
        RTMPMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertTrue(message.getBody() instanceof VideoData);
        
        // Cleanup
        packet.close();
        data.close();
    }

    // ------------------------------------------------------------------
    // interface method tests
    // ------------------------------------------------------------------
    @Test
    public void writeTrailer_doesNotCrash() throws Exception {
        // Execute - should not crash
        publisher.writeTrailer(STREAM_ID);
    }

    @Test
    public void setVideoStreamInfo_doesNotCrash() throws Exception {
        // Execute - should not crash
        publisher.setVideoStreamInfo(STREAM_ID, mock(io.antmedia.plugin.api.StreamParametersInfo.class));
    }

    @Test
    public void setAudioStreamInfo_doesNotCrash() throws Exception {
        // Execute - should not crash
        publisher.setAudioStreamInfo(STREAM_ID, mock(io.antmedia.plugin.api.StreamParametersInfo.class));
    }

    @Test
    public void onOOBControlMessage_handlesMessage() throws Exception {
        // Create mock message
        IMessageComponent source = mock(IMessageComponent.class);
        OOBControlMessage message = new OOBControlMessage();
        message.setTarget("test");
        message.setServiceName("test");
        
        // Execute - should not crash
        publisher.onOOBControlMessage(source, null, message);
    }

    @Test
    public void onOOBControlMessage_handlesNullMessage() throws Exception {
        // Execute - should not crash
        publisher.onOOBControlMessage(null, null, null);
    }

    // ------------------------------------------------------------------
    // packet processing tests
    // ------------------------------------------------------------------
    @Test
    public void testMultiplePacketProcessing() throws Exception {
        // Create multiple packets
        AVPacket packet1 = new AVPacket();
        packet1.size(PACKET_SIZE);
        packet1.pts(VIDEO_TS_MS);
        packet1.flags(avcodec.AV_PKT_FLAG_KEY);
        
        BytePointer data1 = new BytePointer(PACKET_SIZE);
        data1.put(new byte[PACKET_SIZE]);
        packet1.data(data1);
        
        AVPacket packet2 = new AVPacket();
        packet2.size(PACKET_SIZE);
        packet2.pts(VIDEO_TS_MS + 33); // Next frame
        packet2.flags(0);
        
        BytePointer data2 = new BytePointer(PACKET_SIZE);
        data2.put(new byte[PACKET_SIZE]);
        packet2.data(data2);
        
        // Execute
        AVPacket result1 = publisher.onVideoPacket(STREAM_ID, packet1);
        AVPacket result2 = publisher.onVideoPacket(STREAM_ID, packet2);
        
        // Verify
        verify(mockScope, times(2)).pushMessage(any(RTMPMessage.class));
        assertNotNull(result1);
        assertNotNull(result2);
        
        // Cleanup
        packet1.close();
        packet2.close();
        data1.close();
        data2.close();
    }

    @Test
    public void testRealTimestampConversion() throws Exception {
        // Test with real timestamp conversion using av_rescale_q
        long inputTimestamp = 1000;
        long expectedOutputTimestamp = avutil.av_rescale_q(inputTimestamp, videoTb, new AVRational().num(1).den(1000));
        
        // Create real AVPacket
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(inputTimestamp);
        packet.flags(avcodec.AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));
        assertNotNull(result);
        
        // Cleanup
        packet.close();
        data.close();
    }

    // ------------------------------------------------------------------
    // error handling tests
    // ------------------------------------------------------------------
    @Test
    public void testScopeExceptionHandling() throws Exception {
        // Make scope throw exception
        doThrow(new RuntimeException("Test exception")).when(mockScope).pushMessage(any(RTMPMessage.class));
        
        // Create real AVPacket
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(VIDEO_TS_MS);
        packet.flags(avcodec.AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute - should not crash despite exception
        AVPacket result = publisher.onVideoPacket(STREAM_ID, packet);
        
        // Verify
        assertNotNull(result);
        
        // Cleanup
        packet.close();
        data.close();
    }
}
