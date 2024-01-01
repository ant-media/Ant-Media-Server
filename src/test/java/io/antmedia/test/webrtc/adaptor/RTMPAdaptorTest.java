package io.antmedia.test.webrtc.adaptor;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.JavaI420Buffer;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;
import org.webrtc.VideoFrame;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
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
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

public class RTMPAdaptorTest {

	public static final String sampleSdp = "v=0\n" + 
			"o=- 5375254573911477518 2 IN IP4 127.0.0.1\n" + 
			"s=-\n" + 
			"t=0 0\n" + 
			"a=group:BUNDLE audio video\n" + 
			"a=msid-semantic: WMS\n" + 
			"m=audio 9 UDP/TLS/RTP/SAVPF 111 110\n" + 
			"c=IN IP4 0.0.0.0\n" + 
			"a=rtcp:9 IN IP4 0.0.0.0\n" + 
			"a=ice-ufrag:6SBX\n" + 
			"a=ice-pwd:2CKrVFu307y/tXJz64A9Rcl7\n" + 
			"a=ice-options:trickle renomination\n" + 
			"a=fingerprint:sha-256 CA:3C:A8:57:51:36:C2:6A:3A:1E:3C:F5:2A:5C:52:FE:77:A5:C9:98:59:92:08:D6:7D:12:6F:FC:77:3C:88:A1\n" + 
			"a=setup:active\n" + 
			"a=mid:audio\n" + 
			"a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\n" + 
			"a=recvonly\n" + 
			"a=rtcp-mux\n" + 
			"a=rtpmap:111 opus/48000/2\n" + 
			"a=rtcp-fb:111 transport-cc\n" + 
			"a=fmtp:111 minptime=10;useinbandfec=1\n" + 
			"a=rtpmap:110 telephone-event/48000\n" + 
			"m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100\n" + 
			"c=IN IP4 0.0.0.0\n" + 
			"a=rtcp:9 IN IP4 0.0.0.0\n" + 
			"a=ice-ufrag:6SBX\n" + 
			"a=ice-pwd:2CKrVFu307y/tXJz64A9Rcl7\n" + 
			"a=ice-options:trickle renomination\n" + 
			"a=fingerprint:sha-256 CA:3C:A8:57:51:36:C2:6A:3A:1E:3C:F5:2A:5C:52:FE:77:A5:C9:98:59:92:08:D6:7D:12:6F:FC:77:3C:88:A1\n" + 
			"a=setup:active\n" + 
			"a=mid:video\n" + 
			"a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\n" + 
			"a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\n" + 
			"a=extmap:4 urn:3gpp:video-orientation\n" + 
			"a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\n" + 
			"a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\n" + 
			"a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\n" + 
			"a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\n" + 
			"a=recvonly\n" + 
			"a=rtcp-mux\n" + 
			"a=rtcp-rsize\n" + 
			"a=rtpmap:96 H264/90000\n" + 
			"a=rtcp-fb:96 goog-remb\n" + 
			"a=rtcp-fb:96 transport-cc\n" + 
			"a=rtcp-fb:96 ccm fir\n" + 
			"a=rtcp-fb:96 nack\n" + 
			"a=rtcp-fb:96 nack pli\n" + 
			"a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\n" + 
			"a=rtpmap:97 rtx/90000\n" + 
			"a=fmtp:97 apt=96\n" + 
			"a=rtpmap:98 red/90000\n" + 
			"a=rtpmap:99 rtx/90000\n" + 
			"a=fmtp:99 apt=98\n" + 
			"a=rtpmap:100 ulpfec/90000\n";


	private static Logger logger = LoggerFactory.getLogger(RTMPAdaptorTest.class);
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

	@BeforeClass
	public static void beforeClass() {
		
		logger.info("Loading native library - 0");
		PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder()
				.setFieldTrials(null)
				.createInitializationOptions());
		
		logger.info("Loading native library - 1");

	}


	@Test
	public void testOnAddStream() {


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

		verify(webSocketHandler).sendPublishStartedMessage(streamId, session, null, "");
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
			recorder.recordImage(frameCV.getImageWidth(), frameCV.getImageHeight(), frameCV.getImageDepth(),
					frameCV.getImageChannels(), new int[]{1280, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			AVFrame picture = recorder.getPicture();

			assertEquals(1280, picture.linesize(0));
			assertEquals(320, picture.linesize(1));
			assertEquals(320, picture.linesize(2));

			//stop
			recorder.stop();

			assertTrue(MuxingTest.testFile(f.getAbsolutePath()));


			//check frame

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testEncodeDifferentRes() {
		testEncode(640, 480);

		testEncode(480, 360);
	}

	@Test
	public void testAudioOnlyInitialization() 
	{
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), null, 360);

		assertTrue(adaptor.isEnableVideo());
		adaptor.encodeAudio();
		assertNull(adaptor.getRecorder());


		adaptor.setEnableVideo(false);
		assertFalse(adaptor.isEnableVideo());
		adaptor.encodeAudio();
		assertNotNull(adaptor.getRecorder());
		
	}

	public void testEncode(int width, int height) {
		//Create FFmpegFRameRecoder
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), null, height);
		FFmpegFrameRecorder recorder = adaptor.getNewRecorder(f.getAbsolutePath(), width, height, "flv");

		//recorder is started, a new start command throws exception
		try {
			recorder.start();
			fail("It should throw exception");
		} catch (io.antmedia.recorder.FFmpegFrameRecorder.Exception e1) {
			//e1.printStackTrace();
		}

		//give raw frame

		Frame frameCV = new Frame(640, 480, Frame.DEPTH_UBYTE, 2);

		File rawFrameFile = new File("src/test/resources/raw_frame_640_480_yuv420");
		try {
			byte[] rawFrame = Files.readAllBytes(rawFrameFile.toPath());

			((ByteBuffer)(frameCV.image[0].position(0))).put(rawFrame);

			recorder.debugSetStarted(false);

			try {
				recorder.recordImage(frameCV.getImageWidth(), frameCV.getImageHeight(), frameCV.getImageDepth(),
						frameCV.getImageChannels(), new int[]{640, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

				fail("It should throw exception above because started is set to false");
			}
			catch (Exception e) {

			}

			recorder.debugSetStarted(true);
			recorder.recordImage(frameCV.getImageWidth(), frameCV.getImageHeight(), frameCV.getImageDepth(),
					frameCV.getImageChannels(), new int[]{640, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			AVFrame picture = recorder.getPicture();
			assertEquals(width, picture.linesize(0));
			assertEquals(width/2, picture.linesize(1));
			assertEquals(width/2, picture.linesize(2));


			recorder.recordImage(frameCV.getImageWidth(), frameCV.getImageHeight(), frameCV.getImageDepth(),
					frameCV.getImageChannels(), new int[]{640, 320, 320}, AV_PIX_FMT_YUV420P, frameCV.image);

			picture = recorder.getPicture();

			assertEquals(width, picture.linesize(0));
			assertEquals(width/2, picture.linesize(1));
			assertEquals(width/2, picture.linesize(2));

			//stop
			recorder.stop();

			assertTrue(MuxingTest.testFile(f.getAbsolutePath()));


			//check frame

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	public void testVideoAudioQueue() 
	{
		File f = new File("target/test-classes/encoded_frame"+(int)(Math.random()*10010)+".flv");
		RTMPAdaptor adaptor = new RTMPAdaptor(f.getAbsolutePath(), Mockito.mock(WebSocketCommunityHandler.class), 480);

		adaptor.start();
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> adaptor.isStarted());

		WebRTCVideoSink videoSink = adaptor.new WebRTCVideoSink();

		assertEquals(0, adaptor.getVideoFrameQueue().size());

		VideoFrame frame = new VideoFrame(JavaI420Buffer.allocate(360, 240), 0, 0);

		//assertNull(adaptor.getRecorder()); not needed for audio only streams for community edition
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

		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		IScope scope = Mockito.mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		when(adaptor.getScope()).thenReturn(scope);
		when(context.getBean("web.handler")).thenReturn(adaptor);



		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(mock(AppSettings.class));
		WebSocketCommunityHandler webSocketHandler = new WebSocketCommunityHandler(context, null);

		return spy(webSocketHandler);
	}

	@Test
	public void testAddCandidate() {
		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor rtmpAdaptor = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);
		rtmpAdaptor.setRecorder(recorder);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		RemoteEndpoint.Basic  basicRemote = mock(RemoteEndpoint.Basic .class);
		when(session.getBasicRemote()).thenReturn(basicRemote);
		when(session.isOpen()).thenReturn(true);
		rtmpAdaptor.setSession(session);


		rtmpAdaptor.start();
		String sdp = "candidate:78390311 1 udp 2122260223 10.2.40.82 50237 typ host generation 0 ufrag VUE6 network-id 1 network-cost 50";
		//it was crashing here
		rtmpAdaptor.addIceCandidate(new IceCandidate(null, 0, sdp));
		//it was crashing here
		rtmpAdaptor.addIceCandidate(new IceCandidate("audio", 0, null));


		rtmpAdaptor.stop();

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> rtmpAdaptor.getSignallingExecutor().isTerminated());

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


		verify(webSocketHandler).sendTakeCandidateMessage(iceCandidate.sdpMLineIndex, iceCandidate.sdpMid, iceCandidate.sdp, streamId, session, "", "");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, iceCandidate.sdpMLineIndex);
		jsonObject.put(WebSocketConstants.CANDIDATE_ID, iceCandidate.sdpMid);
		jsonObject.put(WebSocketConstants.CANDIDATE_SDP, iceCandidate.sdp);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.LINK_SESSION, "");
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, "");

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
	public void testInitPeerConnectionFactory() {
		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor adaptorReal = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);

		PeerConnectionFactory pcFactory = adaptorReal.createPeerConnectionFactory();
		assertNotNull(pcFactory);

		adaptorReal.initPeerConnection(pcFactory);

		assertNotNull(adaptorReal.getPeerConnection());

	}

	@Test
	public void testDescription() {
		WebSocketCommunityHandler webSocketHandler = getSpyWebSocketHandler();

		RTMPAdaptor adaptor = new RTMPAdaptor("rtmp_url", webSocketHandler, 360);
		Session session = mock(Session.class);
		adaptor.setSession(session);

		adaptor.start();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return adaptor.isStarted();
		});

		SessionDescription sessionDescription = new SessionDescription(Type.OFFER, sampleSdp);
		adaptor.setRemoteDescription(sessionDescription);

		Mockito.verify(webSocketHandler, Mockito.timeout(5000)).sendSDPConfiguration(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


		adaptor.stop();
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

		rtmpAdaptor.setStunServerUri("turn:ovh36.antmedia.io", "ovh36", "ovh36");

		rtmpAdaptor.start();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
		rtmpAdaptor.isStarted()
				);

		verify(webSocketHandler).sendStartMessage(streamId, session, "");

		SessionDescription sdp = new SessionDescription(Type.OFFER, RandomStringUtils.randomAlphanumeric(6));

		rtmpAdaptor.onCreateSuccess(sdp);

		verify(webSocketHandler).sendSDPConfiguration(sdp.description, "offer", streamId, session, null, "", "");
		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, sdp.description);
		jsonResponseObject.put(WebSocketConstants.TYPE, "offer");
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonResponseObject.put(WebSocketConstants.LINK_SESSION, "");
		jsonResponseObject.put(WebSocketConstants.SUBSCRIBER_ID, "");
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

		verify(webSocketHandler).sendPublishFinishedMessage(streamId, session, "");

		JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_FINISHED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObj.put(WebSocketConstants.SUBSCRIBER_ID, "");
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
	public void testRecordSamples() throws io.antmedia.recorder.FFmpegFrameRecorder.Exception {
		RTMPAdaptor adaptor = new RTMPAdaptor("rtmp://", null, 360);

		FFmpegFrameRecorder recorder = Mockito.mock(FFmpegFrameRecorder.class);
		adaptor.setWebRtcAudioTrack(Mockito.mock(WebRtcAudioTrack.class));
	
		ByteBuffer buffer = ByteBuffer.allocate(10);
		adaptor.recordSamples(new AudioFrame(buffer, 1, 16000));


		verify(recorder, Mockito.never()).recordSamples(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

		adaptor.setRecorder(recorder);
		adaptor.recordSamples(new AudioFrame(buffer, 1, 16000));


		verify(recorder).recordSamples(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

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
		assertEquals(0, mock.getNumberOfTotalViewers());
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
