package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;

import javax.websocket.RemoteEndpoint;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;

public class WebSocketCommunityHandlerTest {

	private WebSocketEndpoint wsHandlerReal;
	private WebSocketEndpoint wsHandler;
	private Session session;
	private Basic basicRemote;
	private HashMap userProperties;

	public class WebSocketEndpoint extends WebSocketCommunityHandler {

		@Override
		public ApplicationContext getAppContext() {

			return null;
		}
	}

	@Before
	public void before() {
		wsHandlerReal = new WebSocketEndpoint();
		wsHandler = Mockito.spy(wsHandlerReal);

		session = mock(Session.class);
		basicRemote = mock(RemoteEndpoint.Basic.class);
		when(session.getBasicRemote()).thenReturn(basicRemote);

		userProperties = new HashMap<>();
		when(session.getUserProperties()).thenReturn(userProperties);

		when(session.isOpen()).thenReturn(true);
	}



	@Test
	public void testSendNoStreamId() {

		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		wsHandler.onMessage(session, publishObject.toJSONString());

		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_ID_SPECIFIED);

		try {
			verify(basicRemote).sendText(jsonResponse.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPublishAndDisconnect() {
		String sessionId = String.valueOf((int)(Math.random()*10000));


		when(session.getId()).thenReturn(sessionId);
		wsHandler.onOpen(session, null);


		String streamId = "streamId" + (int)(Math.random()*1000);

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString());


		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(rtmpAdaptor).setSession(session);
		verify(rtmpAdaptor).setStreamId(streamId);
		verify(rtmpAdaptor).start();

		wsHandler.onClose(session);

		verify(rtmpAdaptor).stop();

	}

	@Test
	public void testPublishAndStopCommand() {

		String sessionId = String.valueOf((int)(Math.random()*10000));


		when(session.getId()).thenReturn(sessionId);
		wsHandler.onOpen(null, null);


		String streamId = "streamId" + (int)(Math.random()*1000);

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString());


		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(rtmpAdaptor).setSession(session);
		verify(rtmpAdaptor).setStreamId(streamId);
		verify(rtmpAdaptor).start();

		{
			//send take configuration command
			JSONObject takeCandidate = new JSONObject();
			takeCandidate.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
			String type = "offer";
			takeCandidate.put(WebSocketConstants.TYPE, type);
			String sdp = "sdp" + (int)(Math.random() * 91000);
			takeCandidate.put(WebSocketConstants.SDP, sdp);
			takeCandidate.put(WebSocketConstants.STREAM_ID, streamId);

			wsHandler.onMessage(session, takeCandidate.toJSONString());

			ArgumentCaptor<SessionDescription> argument = ArgumentCaptor.forClass(SessionDescription.class);
			verify(rtmpAdaptor).setRemoteDescription(argument.capture());
			SessionDescription sessionDescription = argument.getValue();
			assertEquals(Type.OFFER, sessionDescription.type);
			assertEquals(sdp, sessionDescription.description);
		}

		//send take candidate command 
		{
			JSONObject takeCandidate = new JSONObject();
			takeCandidate.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);
			String type = ""  +(int)(Math.random() * 91000);
			takeCandidate.put(WebSocketConstants.CANDIDATE_ID, type );
			String sdp = ""  +(int)(Math.random() * 91000);;
			takeCandidate.put(WebSocketConstants.CANDIDATE_SDP, sdp);
			int label = (int)(Math.random() * 91000);;
			takeCandidate.put(WebSocketConstants.CANDIDATE_LABEL, label);
			takeCandidate.put(WebSocketConstants.STREAM_ID, streamId);

			wsHandler.onMessage(session, takeCandidate.toJSONString());

			ArgumentCaptor<IceCandidate> argument = ArgumentCaptor.forClass(IceCandidate.class);
			verify(rtmpAdaptor).addIceCandidate(argument.capture());
			IceCandidate icecandidate = argument.getValue();
			assertEquals(sdp, icecandidate.sdp);
			assertEquals(type,icecandidate.sdpMid);
			assertEquals(label,icecandidate.sdpMLineIndex);
			

		}


		JSONObject stopObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.STOP_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(rtmpAdaptor).stop();
	}
	
}
