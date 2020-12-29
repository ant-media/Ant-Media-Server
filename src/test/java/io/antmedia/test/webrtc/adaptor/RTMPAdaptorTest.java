package io.antmedia.test.webrtc.adaptor;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.JavaI420Buffer;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoFrame;
import org.webrtc.SessionDescription.Type;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.integration.MuxingTest;
import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.webrtc.AudioFrameContext;
import io.antmedia.webrtc.MockWebRTCAdaptor;
import io.antmedia.webrtc.VideoCodec;
import io.antmedia.webrtc.VideoFrameContext;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.webrtc.adaptor.RTMPAdaptor.AudioFrame;
import io.antmedia.webrtc.adaptor.RTMPAdaptor.WebRTCVideoSink;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;

public class RTMPAdaptorTest {
	
	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@Before
	public void setup() {

	}


	@Test
	public void testOnAddStream() {

		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandler = mock(WebSocketCommunityHandler.class);

		RTMPAdaptor adaptorReal = new RTMPAdaptor("rtmp://url", webSocketHandler, 360);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		rtmpAdaptor.setSession(session);

		MediaStream stream =  new MediaStream(0L);
		rtmpAdaptor.onAddStream(stream);

		/* no room property is put to session with streamId, because roomName is put during joining to room  
		 * getting room parameter from session is tested in io.antmedia.test.enterprise.WebSocketHandlerUnitTest.joinConferenceRoomAndPublish
		 */
		assertNull(session.getUserProperties().get(streamId));

		verify(webSocketHandler).sendPublishStartedMessage(streamId, session, null);
	}

	
	@Test
	public void testUnexpectedLineSize() {
		//Create FFmpegFRameRecoder
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), null, 480);
		FFmpegFrameRecorder recorder = adaptor.getNewRecorder(f.getAbsolutePath(), 640, 480, "flv");

		//give raw frame

		Frame frameCV = new Frame(640, 480, Frame.DEPTH_UBYTE, 2);

		//this raw frame is 640x480, yuv420p
		//ffplay -f rawvideo -pixel_format yuv420p -video_size 640x480 -i raw_frame_640_480_yuv420
		File rawFrameFile = new File("src/test/resources/raw_frame_640_480_yuv420");
		try {
			byte[] rawFrame = Files.readAllBytes(rawFrameFile.toPath());
			
			((ByteBuffer)(frameCV.image[0].position(0))).put(rawFrame);
			
			//this is false to give 1280, 320, 320 but it let us know it is effective
			recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
					frameCV.imageChannels, new int[]{1280, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			AVFrame picture = recorder.getPicture();
			
			assertEquals(1280, picture.linesize(0));
			assertEquals(320, picture.linesize(1));
			assertEquals(320, picture.linesize(2));
			
			//stop
			recorder.stop();
			
			assertTrue(MuxingTest.testFile(f.getAbsolutePath()));
			

			//check frame

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testEncodeDifferentRes() {
		testEncode(640, 480);
		
		testEncode(480, 360);
	}
	
	public void testEncode(int width, int height) {
		//Create FFmpegFRameRecoder
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), null, height);
		FFmpegFrameRecorder recorder = adaptor.getNewRecorder(f.getAbsolutePath(), width, height, "flv");

		//give raw frame

		Frame frameCV = new Frame(640, 480, Frame.DEPTH_UBYTE, 2);

		File rawFrameFile = new File("src/test/resources/raw_frame_640_480_yuv420");
		try {
			byte[] rawFrame = Files.readAllBytes(rawFrameFile.toPath());
			
			((ByteBuffer)(frameCV.image[0].position(0))).put(rawFrame);

			recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
					frameCV.imageChannels, new int[]{640, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			AVFrame picture = recorder.getPicture();
			assertEquals(width, picture.linesize(0));
			assertEquals(width/2, picture.linesize(1));
			assertEquals(width/2, picture.linesize(2));
			
			
			recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
					frameCV.imageChannels, new int[]{640, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			picture = recorder.getPicture();
			
			assertEquals(width, picture.linesize(0));
			assertEquals(width/2, picture.linesize(1));
			assertEquals(width/2, picture.linesize(2));
			
			//stop
			recorder.stop();
			
			assertTrue(MuxingTest.testFile(f.getAbsolutePath()));
			

			//check frame

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}


	}
	
	@Test
	public void testVideoAudioQueue() 
	{
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), Mockito.mock(WebSocketCommunityHandler.class), 480);
		
		PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder().createInitializationOptions());
		
		adaptor.start();
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.isStarted());
		
		WebRTCVideoSink videoSink = adaptor.new WebRTCVideoSink();
		
		assertEquals(0, adaptor.getVideoFrameQueue().size());
		
		VideoFrame frame = new VideoFrame(JavaI420Buffer.allocate(360, 240), 0, 0);
		
		assertNull(adaptor.getRecorder());
		videoSink.onFrame(frame);
		
		adaptor.getAudioFrameQueue().offer(new AudioFrame(ByteBuffer.allocateDirect(1024), 1, 16000));
		
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.getRecorder() != null);
				
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !adaptor.getSignallingExecutor().isShutdown());
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !adaptor.getVideoEncoderExecutor().isShutdown());
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !adaptor.getAudioEncoderExecutor().isShutdown());
		
		adaptor.stop();
		
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.getSignallingExecutor().isShutdown());
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.getVideoEncoderExecutor().isShutdown());
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.getAudioEncoderExecutor().isShutdown());
		
		
	}
	
	@Test
	public void testVideoDecoderFactory() {
		//Video decoder factory should return null otherwise it does not work
		RTMPAdaptor rtmpAdaptor = new RTMPAdaptor(null, null, 0);
		assertNull(rtmpAdaptor.getVideoDecoderFactory());
	}


	@Test
	public void testIsStarted() {
		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor rtmpAdaptor = new RTMPAdaptor("rtmp url", webSocketHandler, 360);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		rtmpAdaptor.setSession(session);

		assertNull(rtmpAdaptor.getAudioDataSchedulerFuture());
		assertEquals(0, rtmpAdaptor.getStartTime());

		rtmpAdaptor.start();


		Awaitility.await().pollDelay(1, TimeUnit.SECONDS)
		.atMost(20, TimeUnit.SECONDS)
		.until(() -> rtmpAdaptor.isStarted());


		rtmpAdaptor.initAudioTrackExecutor();

		assertNotNull(rtmpAdaptor.getAudioDataSchedulerFuture());

		rtmpAdaptor.stop();

		Awaitility.await().pollDelay(1, TimeUnit.SECONDS)
		.atMost(20, TimeUnit.SECONDS)
		.until(() -> rtmpAdaptor.getAudioDataSchedulerFuture().isCancelled());

		assertTrue(rtmpAdaptor.getAudioDataSchedulerFuture().isCancelled());

		Awaitility.await().pollDelay(1, TimeUnit.SECONDS)
		.atMost(20, TimeUnit.SECONDS)
		.until(() -> rtmpAdaptor.isStopped());

	}

	private WebSocketCommunityHandler getSpyWebSocketHandler() {
		
		ApplicationContext context = mock(ApplicationContext.class);
		
		IApplicationAdaptorFactory appFactory = Mockito.mock(IApplicationAdaptorFactory.class);
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		when(appFactory.getAppAdaptor()).thenReturn(adaptor);
		IScope scope = Mockito.mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		when(adaptor.getScope()).thenReturn(scope);
		when(context.getBean("web.handler")).thenReturn(appFactory);

		
		
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(mock(AppSettings.class));
		WebSocketCommunityHandler webSocketHandler = new WebSocketCommunityHandler(context, null);

		return spy(webSocketHandler);
	}


	@Test
	public void testCandidate() {
		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor adaptorReal = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		RemoteEndpoint.Basic  basicRemote = mock(RemoteEndpoint.Basic .class);
		when(session.getBasicRemote()).thenReturn(basicRemote);
		when(session.isOpen()).thenReturn(true);
		rtmpAdaptor.setSession(session);

		IceCandidate iceCandidate = new IceCandidate(RandomStringUtils.randomAlphanumeric(6), 5, RandomStringUtils.randomAlphanumeric(6));
		rtmpAdaptor.onIceCandidate(iceCandidate);


		verify(webSocketHandler).sendTakeCandidateMessage(iceCandidate.sdpMLineIndex, iceCandidate.sdpMid, iceCandidate.sdp, streamId, session);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, iceCandidate.sdpMLineIndex);
		jsonObject.put(WebSocketConstants.CANDIDATE_ID, iceCandidate.sdpMid);
		jsonObject.put(WebSocketConstants.CANDIDATE_SDP, iceCandidate.sdp);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		try {
			verify(basicRemote).sendText(jsonObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}
	
	@Test
	public void testCallStopMultipletime() {
		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor adaptorReal = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);
		
		adaptorReal.setSession(mock(Session.class));
		
		adaptorReal.start();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> adaptorReal.isStarted());
		
		adaptorReal.stop();
		
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> adaptorReal.getSignallingExecutor().isShutdown());
		
		adaptorReal.stop();
		
	}

	@Test
	public void testStartandStop() {

		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor adaptorReal = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		RemoteEndpoint.Basic  basicRemote = mock(RemoteEndpoint.Basic .class);
		when(session.getBasicRemote()).thenReturn(basicRemote);
		when(session.isOpen()).thenReturn(true);
		rtmpAdaptor.setSession(session);

		PeerConnectionFactory peerConnectionFactory = mock(PeerConnectionFactory.class);

		doReturn(peerConnectionFactory).when(rtmpAdaptor).createPeerConnectionFactory();

		rtmpAdaptor.start();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
		rtmpAdaptor.isStarted()
				);

		verify(webSocketHandler).sendStartMessage(streamId, session);

		SessionDescription sdp = new SessionDescription(Type.OFFER, RandomStringUtils.randomAlphanumeric(6));

		rtmpAdaptor.onCreateSuccess(sdp);

		verify(webSocketHandler).sendSDPConfiguration(sdp.description, "offer", streamId, session);
		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, sdp.description);
		jsonResponseObject.put(WebSocketConstants.TYPE, "offer");
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);
		try {
			verify(basicRemote).sendText(jsonResponseObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		rtmpAdaptor.stop();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
		rtmpAdaptor.getSignallingExecutor().isShutdown()
				);

		verify(webSocketHandler).sendPublishFinishedMessage(streamId, session);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_FINISHED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);
		try {
			verify(basicRemote).sendText(jsonObj.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testGetFileFormat() {


		FFmpegFrameRecorder recorder = RTMPAdaptor.initRecorder("rtmp://test", 640, 480, "flv");

		assertEquals("flv", recorder.getFormat());
	}

	@Test
	public void testNoAudioNoVideoInStream() {

		try {

			WebSocketCommunityHandler handler = mock(WebSocketCommunityHandler.class);

			RTMPAdaptor rtmpAdaptor = new RTMPAdaptor(null, handler, 360);

			MediaStream stream = new MediaStream(0L);

			Session session = mock(Session.class);

			rtmpAdaptor.setSession(session);

			rtmpAdaptor.onAddStream(stream);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}
	
	@Test
	public void testRecordSamples() {
		RTMPAdaptor adaptor = new RTMPAdaptor("rtmp://", null, 360);
		
		FFmpegFrameRecorder recorder = Mockito.mock(FFmpegFrameRecorder.class);
		adaptor.setWebRtcAudioTrack(Mockito.mock(WebRtcAudioTrack.class));
		
		ByteBuffer buffer = ByteBuffer.allocate(10);
		adaptor.recordSamples(new AudioFrame(buffer, 1, 16000));
		
		try {
			verify(recorder, Mockito.never()).recordSamples(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
		} catch (io.antmedia.recorder.FrameRecorder.Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		adaptor.setRecorder(recorder);
		adaptor.recordSamples(new AudioFrame(buffer, 1, 16000));
		
		try {
			verify(recorder).recordSamples(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
		} catch (io.antmedia.recorder.FrameRecorder.Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testAudioVideoFrames() 
	{
		JavaI420Buffer buffer = Mockito.mock(JavaI420Buffer.class);
		VideoFrame vframe = new VideoFrame(buffer, 90, 100);
		VideoFrameContext videoFrameContext = new VideoFrameContext(vframe, 900);
		
		assertEquals(vframe, videoFrameContext.videoFrame);
		assertEquals(900, videoFrameContext.timestampMS);
		
		byte[] data = new byte[100];
		AudioFrameContext aframeContext = new AudioFrameContext(data, 10, 20, 30, 40);
		assertEquals(data, aframeContext.data);
		assertEquals(10, aframeContext.timestampMs);
		assertEquals(20, aframeContext.numberOfFrames);
		assertEquals(30, aframeContext.channels);
		assertEquals(40, aframeContext.sampleRate);
		
	}
	
	@Test
	public void testInitializeRecorder() {
		
		String rtmpUrl = "rtmp://"+(int)(Math.random()*10000);
		
		Session session = Mockito.mock(Session.class);
		WebSocketCommunityHandler handler = getSpyWebSocketHandler(); //Mockito.spy(new WebSocketCommunityHandler(null, session));
		handler.setSession(session);
		
		RTMPAdaptor adaptor = new RTMPAdaptor(rtmpUrl, handler, 480);
		String streamId = "stream" + (int)(Math.random()*1000);
		adaptor.setStreamId(streamId);
		adaptor.setSession(session);
		RTMPAdaptor adaptorSpy = Mockito.spy(adaptor);
		
		VideoFrame frame = Mockito.mock(VideoFrame.class);
		when(frame.getRotatedWidth()).thenReturn(480);
		when(frame.getRotatedHeight()).thenReturn(360);
		
		Mockito.doNothing().when(adaptorSpy).stop();
				
		adaptorSpy.initializeRecorder(frame);
		verify(adaptorSpy).getNewRecorder(rtmpUrl, 640, 480, "flv");
		
		//stop should be called because rtmp url is not valid
		verify(adaptorSpy).stop();
		
		adaptorSpy.initializeRecorder(frame);
		verify(adaptorSpy, Mockito.times(1)).getNewRecorder(rtmpUrl, 640, 480, "flv");
		verify(handler).sendServerError(streamId, session);
		
	}
	
	
	
	/*
	 * This test is only for sonar coverage for now. Because tested class is mock and not doing anything
	 */
	@Test
	public void testMockWebRTCAdaptor() {
		MockWebRTCAdaptor mock = new MockWebRTCAdaptor();
		mock.registerMuxer(null, null);
		mock.unRegisterMuxer(null, null);
		mock.registerWebRTCClient(null, null, VideoCodec.H264);
		mock.streamExists(null);
		mock.getStreamInfo(null);
		mock.adaptStreamingQuality(null, null, null);
		mock.registerWebRTCClient(null, null, 0, null);
		assertEquals(-1, mock.getNumberOfLiveStreams());
		assertEquals(-1, mock.getNumberOfTotalViewers());
		assertEquals(-1, mock.getNumberOfViewers(null));
		assertTrue(mock.getWebRTCClientStats(null).isEmpty());
		assertTrue(mock.getStreams().isEmpty());
		mock.setExcessiveBandwidthValue(0);
		mock.setExcessiveBandwidthCallThreshold(0);
		mock.setExcessiveBandwidthAlgorithmEnabled(true);
		mock.setPacketLossDiffThresholdForSwitchback(0);
		mock.setRttMeasurementDiffThresholdForSwitchback(0);
		mock.setTryCountBeforeSwitchback(0);
		mock.forceStreamingQuality(null,null,0);
	}
}
