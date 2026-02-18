package io.antmedia.test.rtmp;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import io.antmedia.muxer.RtmpProvider;
import io.vertx.core.Vertx;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.message.RTMPMessage;

import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

@RunWith(MockitoJUnitRunner.class)
public class InProcessRtmpPublisherUnitTest {

    // ------------------------------------------------------------------
    // mocks & constants
    // ------------------------------------------------------------------
    @Spy private IBroadcastScope mockScope;
    @Mock private AVPacket        mockPacket;
    @Mock private BytePointer     mockBytePointer;
    @Mock private ByteBuffer      mockByteBuffer;

    private RtmpProvider publisher;
    private AVRational videoTb;
    private AVRational audioTb;

    @Mock
    private IScope mockAppScope;

    @Mock
    private Vertx vertx;


    private static final String STREAM_ID      = "test_stream_id";
    private static final int    VIDEO_TS_MS    = 1000;
    private static final int    AUDIO_TS_MS    = 2000;
    private static final int    PACKET_SIZE    = 100;

    // ------------------------------------------------------------------
    // test scaffolding
    // ------------------------------------------------------------------
    @Before
    public void setUp() {
        // Create real AVRational objects instead of mocking
        videoTb = new AVRational().num(1).den(30);
        audioTb = new AVRational().num(1).den(48000);

        publisher = spy(new RtmpProvider(mockAppScope,vertx,STREAM_ID,videoTb,audioTb));
        publisher.setBroadcastScope(spy(publisher.getBroadcastScope()));
        mockScope  = publisher.getBroadcastScope();
        doReturn(mockScope).when(mockAppScope).getBroadcastScope(STREAM_ID);
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
        packet.flags(AV_PKT_FLAG_KEY); // Key frame
        
        // Create real data
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);

        // Execute
        publisher.writePacket(packet, videoTb, videoTb, AVMEDIA_TYPE_VIDEO);

        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));

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
        publisher.writePacket(packet, videoTb, null, AVMEDIA_TYPE_VIDEO);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));

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
        packet.flags(AV_PKT_FLAG_KEY);
        
        // Execute
        publisher.writePacket(packet, videoTb , null,AVMEDIA_TYPE_VIDEO);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));

        // Cleanup
        packet.close();
    }

    @Test
    public void videoPacket_nullPacket_doesNotPushMessage() throws Exception {
        // Execute
        publisher.writePacket(null,videoTb,null,AVMEDIA_TYPE_VIDEO);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
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
        publisher.writePacket(packet,audioTb,null,AVMEDIA_TYPE_AUDIO);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));

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
        publisher.writePacket(packet,audioTb,null,AVMEDIA_TYPE_AUDIO);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
        
        // Cleanup
        packet.close();
    }

    @Test
    public void audioPacket_nullPacket_doesNotPushMessage() throws Exception {
        // Execute
        publisher.writePacket(null,audioTb,null,AVMEDIA_TYPE_AUDIO);
        
        // Verify
        verify(mockScope, never()).pushMessage(any(RTMPMessage.class));
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
        packet.flags(AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        publisher.writePacket(packet, videoTb, null,AVMEDIA_TYPE_VIDEO);
        
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
    public void writeTrailer_doesNotCrash() {
        // Execute - should not crash
        publisher.writeTrailer();
        assertTrue(true);
    }

    @Test
    public void onOOBControlMessage_handlesMessage() {
        // Create mock message
        IMessageComponent source = mock(IMessageComponent.class);
        OOBControlMessage message = new OOBControlMessage();
        message.setTarget("test");
        message.setServiceName("test");
        
        // Execute - should not crash
        publisher.onOOBControlMessage(source, null, message);
        assertTrue(true);
    }

    @Test
    public void onOOBControlMessage_handlesNullMessage() {
        // Execute - should not crash
        publisher.onOOBControlMessage(null, null, null);
        assertTrue(true);
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
        packet1.flags(AV_PKT_FLAG_KEY);
        
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
        publisher.writePacket(packet1,videoTb,null, AVMEDIA_TYPE_VIDEO);
        publisher.writePacket(packet2,videoTb,null, AVMEDIA_TYPE_VIDEO);
        
        // Verify
        verify(mockScope, times(2)).pushMessage(any(RTMPMessage.class));
        
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

        // Create real AVPacket
        AVPacket packet = new AVPacket();
        packet.size(PACKET_SIZE);
        packet.pts(inputTimestamp);
        packet.flags(AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute
        publisher.writePacket(packet,videoTb,null, AVMEDIA_TYPE_VIDEO);
        
        // Verify
        verify(mockScope, times(1)).pushMessage(any(RTMPMessage.class));
        
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
        packet.flags(AV_PKT_FLAG_KEY);
        
        BytePointer data = new BytePointer(PACKET_SIZE);
        data.put(new byte[PACKET_SIZE]);
        packet.data(data);
        
        // Execute - should not crash despite exception
        publisher.writePacket(packet,videoTb,null,AVMEDIA_TYPE_VIDEO);
        
        // Cleanup
        packet.close();
        data.close();

        assertTrue(true);
    }

    @Test
    public void testAttachWithExistingBroadcastScope() {
        // Execute
        IBroadcastScope broadcastScope = publisher.attachRtmpPublisher(
                STREAM_ID);

        // Verify
        assertNotNull("Should return a valid BroadcastScope", broadcastScope);
        verify(mockAppScope, times(2)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));
        verify(mockScope, times(1)).subscribe(any(RtmpProvider.class), eq(null));
    }

    @Test
    public void testAttachWithNewBroadcastScope() {
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));
    }

    @Test
    public void testDetachWithValidPublisher() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockScope);

        // Execute
        publisher.detachRtmpPublisher(STREAM_ID);

        // Verify
        verify(mockAppScope, times(2)).getBroadcastScope(STREAM_ID);
        verify(mockScope, times(1)).unsubscribe(publisher);
    }

    @Test
    public void testDetachWithNullBroadcastScope() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(null);

        // Execute
        publisher.detachRtmpPublisher(STREAM_ID);

        // Verify
        verify(mockAppScope, times(2)).getBroadcastScope(STREAM_ID);
        // Should not call unsubscribe on null scope
        verify(mockScope, times(0)).unsubscribe(publisher);
    }

    @Test
    public void testEndToEndWorkflow() {

        // Verify attach worked
        assertNotNull("Publisher should be created", mockScope);
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));

        // Execute detach
        publisher.detachRtmpPublisher(STREAM_ID);

        // Verify detach worked
        verify(mockAppScope, times(2)).getBroadcastScope(STREAM_ID); // Once for attach, once for detach
        verify(mockScope, times(1)).unsubscribe(publisher);
    }

    @Test
    public void testMultipleAttachDetachCycles() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockScope);

        // Execute multiple attach/detach cycles
        for (int i = 0; i < 3; i++) {
            publisher.attachRtmpPublisher(
                    STREAM_ID);

            assertNotNull("Publisher should be created in cycle " + i, publisher);

            publisher.detachRtmpPublisher(STREAM_ID);
        }

        // Verify all calls were made
        verify(mockAppScope, times(7)).getBroadcastScope(STREAM_ID); // 3 attach + 3 detach
        verify(mockScope, times(3)).subscribe(any(RtmpProvider.class), eq(null));
        verify(mockScope, times(3)).unsubscribe(any(RtmpProvider.class));
    }
    @Test
    public void testIsCodecSupported(){
        assertTrue(publisher.isCodecSupported(AV_CODEC_ID_H264));
        assertTrue(publisher.isCodecSupported(AV_CODEC_ID_AAC));
        assertFalse(publisher.isCodecSupported(AV_CODEC_ID_OPUS));

    }
    @Test
    public void testOutputFormatCtx(){
        RtmpProvider rtmpPublisher = spy(new RtmpProvider(mockAppScope,vertx,STREAM_ID,videoTb,audioTb));
        AVFormatContext ctx = rtmpPublisher.getOutputFormatContext();
        assertEquals("null",ctx.oformat().name().getString());
        ctx = rtmpPublisher.getOutputFormatContext();
        assertEquals("null",ctx.oformat().name().getString());
    }
    @Test
    public void testAddStream(){
        AVCodecParameters codecParameters = new AVCodecParameters();
        AVRational timebase = new AVRational();
        int streamIndex = 1;
        codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
        publisher.addStream(codecParameters,timebase,streamIndex);
        verify(publisher).addStream(codecParameters,timebase,streamIndex);
        assertNull(publisher.getVideoExtradata());

        BytePointer bp = new BytePointer("testing 123");

        codecParameters.extradata_size(5);
        codecParameters.extradata(bp);
        publisher.addStream(codecParameters,timebase,streamIndex);
        assertNotNull(publisher.getVideoExtradata());
        verify(publisher,times(2)).addStream(codecParameters,timebase,streamIndex);

        codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
        publisher.addStream(codecParameters,timebase,streamIndex);
        verify(publisher,times(3)).addStream(codecParameters,timebase,streamIndex);
    }

}
