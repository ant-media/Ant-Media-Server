package io.antmedia.test.webrtc.adaptor;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.red5.net.websocket.WebSocketConnection;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

public class RTMPAdaptorTest {

	@Before
	public void setup() {

	}


	@Test
	public void testOnAddStream() {

		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandlerReal = new WebSocketCommunityHandler() {

			@Override
			public ApplicationContext getAppContext() {
				return null;
			}
		};

		WebSocketCommunityHandler webSocketHandler = spy(webSocketHandlerReal);

		RTMPAdaptor adaptorReal = new RTMPAdaptor(recorder, webSocketHandler);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		rtmpAdaptor.setSession(session);

		MediaStream stream = mock(MediaStream.class);
		rtmpAdaptor.onAddStream(stream);

		verify(webSocketHandler).sendPublishStartedMessage(streamId, session);
	}

	@Test
	public void testCandidate() {
		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandlerReal = new WebSocketCommunityHandler() {

			@Override
			public ApplicationContext getAppContext() {
				return null;
			}
		};

		WebSocketCommunityHandler webSocketHandler = spy(webSocketHandlerReal);

		RTMPAdaptor adaptorReal = new RTMPAdaptor(recorder, webSocketHandler);
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
	public void testStartandStop() {

		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandlerReal = new WebSocketCommunityHandler() {

			@Override
			public ApplicationContext getAppContext() {
				return null;
			}
		};

		WebSocketCommunityHandler webSocketHandler = spy(webSocketHandlerReal);

		RTMPAdaptor adaptorReal = new RTMPAdaptor(recorder, webSocketHandler);
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


		FFmpegFrameRecorder recorder = WebSocketCommunityHandler.initRecorder("rtmp://test");

		assertEquals("flv", recorder.getFormat());
	}

	@Test
	public void testNoAudioNoVideoInStream() {

		try {

			WebSocketCommunityHandler handler = mock(WebSocketCommunityHandler.class);

			RTMPAdaptor rtmpAdaptor = new RTMPAdaptor(null, handler);

			MediaStream stream = mock(MediaStream.class);

			Session session = mock(Session.class);

			rtmpAdaptor.setSession(session);

			rtmpAdaptor.onAddStream(stream);


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

}
