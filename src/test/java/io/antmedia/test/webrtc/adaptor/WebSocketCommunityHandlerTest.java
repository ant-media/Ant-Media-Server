package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;

import javax.websocket.RemoteEndpoint;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;


import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;

public class WebSocketCommunityHandlerTest {

	private WebSocketEndpoint wsHandlerReal;
	private WebSocketEndpoint wsHandler;
	private Session session;
	private Basic basicRemote;
	private HashMap userProperties;
	private ApplicationContext appContext;
	private DataStore dataStore;
	
	public class WebSocketEndpoint extends WebSocketCommunityHandler {
		public WebSocketEndpoint(ApplicationContext appContext) {
			super(appContext, null);
			// TODO Auto-generated constructor stub
		}
		
		public void setSession(Session session) {
			this.session = session;
		}

		@Override
		public ApplicationContext getAppContext() {

			return appContext;
		}
	}
	
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
	public void before() {
		appContext = Mockito.mock(ApplicationContext.class);
		when(appContext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		IApplicationAdaptorFactory appFactory = Mockito.mock(IApplicationAdaptorFactory.class);
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		when(appFactory.getAppAdaptor()).thenReturn(adaptor);
		IScope scope = Mockito.mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		when(adaptor.getScope()).thenReturn(scope);
	
		when(appContext.getBean("web.handler")).thenReturn(appFactory);
		
		wsHandlerReal = new WebSocketEndpoint(appContext);
		wsHandlerReal.setAppAdaptor(adaptor);
		
		dataStore = new InMemoryDataStore("junit");
		when(adaptor.getDataStore()).thenReturn(dataStore);
		
		wsHandler = Mockito.spy(wsHandlerReal);

		
		session = mock(Session.class);
		basicRemote = mock(RemoteEndpoint.Basic.class);
		when(session.getBasicRemote()).thenReturn(basicRemote);
		

		userProperties = new HashMap<>();
		when(session.getUserProperties()).thenReturn(userProperties);

		when(session.isOpen()).thenReturn(true);
	}


	@Test
	public void testPingPong() {
		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PING_COMMAND);
		
		wsHandler.onMessage(session, publishObject.toJSONString());
		
		verify(wsHandler).sendPongMessage(session);
		
		verify(wsHandler, Mockito.never()).sendNoStreamIdSpecifiedError(session);
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
	public void testSendStreamIdInUse() {
		
		//case status broadcasting
		String streamId = "streamId" + (int)(Math.random()*10000);
		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		dataStore.save(broadcast);
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler).sendStreamIdInUse(session);
		
		
		//case status preparing
		streamId = "streamId" + (int)(Math.random()*10000);
		broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		dataStore.save(broadcast);
		
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(session);
		
		
		// case no status
		streamId = "streamId" + (int)(Math.random()*10000);
		broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		dataStore.save(broadcast);
		
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(session);
		
		
		
		// case no status
		streamId = "streamId" + (int)(Math.random()*10000);

		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(session);
				
		
	}
	
	@Test
	public void testGetStreamInfo() {
		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_STREAM_INFO_COMMAND);
		
		String streamId = "streamId" + (int)(Math.random()*1000);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		
		wsHandler.onMessage(session, publishObject.toJSONString());
		
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.ERROR_CODE, "404");
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_EXIST);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		
		try {
			verify(basicRemote).sendText(jsonResponse.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testGetNewRTMPAdaptor() {
		String rtmpUrl = "rtmp://localhost/LiveApp/232323";
		int height = 260;
		RTMPAdaptor rtmpAdaptor = wsHandler.getNewRTMPAdaptor(rtmpUrl, height);
		
		assertEquals(height, rtmpAdaptor.getHeight());
		assertEquals(rtmpUrl, rtmpAdaptor.getOutputURL());
	}

	@Test
	public void testPublishAndDisconnect() {
		String sessionId = String.valueOf((int)(Math.random()*10000));
		when(session.getId()).thenReturn(sessionId);

		String streamId = "streamId" + (int)(Math.random()*1000);

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString(), Mockito.anyInt());


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

		String streamId = "streamId" + (int)(Math.random()*1000);

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString(), Mockito.anyInt());


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
	
	@Test
	public void testInvalidName() {

		String sessionId = String.valueOf((int)(Math.random()*10000));


		when(session.getId()).thenReturn(sessionId);

		String streamId = "streamId" + (int)(Math.random()*1000);

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString(), Mockito.anyInt());


		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		wsHandler.onMessage(session, publishObject.toJSONString());
		
		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.any());
		
		String streamId2 = "streamId_" + (int)(Math.random()*1000);
		JSONObject publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());
		
		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.any());
		
		streamId2 = "streamId_:?" + (int)(Math.random()*1000);
		publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());
		
		verify(wsHandler, Mockito.timeout(1)).sendInvalidStreamNameError(Mockito.any());
	}
	
	@Test
	public void testWebSocketConstants() {
		assertEquals("already_playing", WebSocketConstants.ALREADY_PLAYING);
		assertEquals("targetBitrate", WebSocketConstants.TARGET_BITRATE);
		assertEquals("bitrateMeasurement", WebSocketConstants.BITRATE_MEASUREMENT);
	}
	
	@Test
	public void testSendRoomInformation() 
	{
		String roomId = "roomId12345";
		JSONArray jsonStreamArray = new JSONArray();
		jsonStreamArray.add("stream1");
		jsonStreamArray.add("stream2");
		jsonStreamArray.add("stream3");
		jsonStreamArray.add("stream4");
		wsHandler.setSession(session);
		wsHandler.sendRoomInformation(jsonStreamArray, roomId);
		
		
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));
		
		JSONObject json;
		try {
			json = (JSONObject) new JSONParser().parse(argument.getValue());
			assertEquals(WebSocketConstants.ROOM_INFORMATION_NOTIFICATION, json.get(WebSocketConstants.COMMAND));	
			assertEquals(roomId, json.get(WebSocketConstants.ROOM));
			assertEquals(jsonStreamArray, json.get(WebSocketConstants.STREAMS_IN_ROOM));
			
		} catch (ParseException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSendJoinedRoomInformation() {
		String roomId = "roomId12345";
		String streamId = "stream34567";
		JSONArray jsonStreamArray = new JSONArray();
		jsonStreamArray.add("stream1");
		jsonStreamArray.add("stream2");
		jsonStreamArray.add("stream3");
		jsonStreamArray.add("stream4");
		wsHandler.setSession(session);
		
		wsHandler.sendJoinedRoomMessage(roomId, streamId, jsonStreamArray);
		
		
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));
		
		JSONObject json;
		try {
			json = (JSONObject) new JSONParser().parse(argument.getValue());
			assertEquals(WebSocketConstants.NOTIFICATION_COMMAND, json.get(WebSocketConstants.COMMAND));	
			assertEquals(roomId, json.get(WebSocketConstants.ROOM));
			assertEquals(jsonStreamArray, json.get(WebSocketConstants.STREAMS_IN_ROOM));
			assertEquals(WebSocketConstants.JOINED_THE_ROOM, json.get(WebSocketConstants.DEFINITION));
			assertEquals(streamId, json.get(WebSocketConstants.STREAM_ID));
			
		} catch (ParseException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSendPublishStarted() {
		String roomId = "roomId12345";
		String streamId = "stream34567";
		JSONArray jsonStreamArray = new JSONArray();
		
		wsHandler.setSession(session);
		
		wsHandler.sendPublishStartedMessage(streamId,  session, roomId); 
		
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));
		
		JSONObject json;
		try {
			json = (JSONObject) new JSONParser().parse(argument.getValue());
			assertEquals(WebSocketConstants.NOTIFICATION_COMMAND, json.get(WebSocketConstants.COMMAND));	
			assertEquals(roomId, json.get(WebSocketConstants.ROOM));
			assertEquals(WebSocketConstants.PUBLISH_STARTED, json.get(WebSocketConstants.DEFINITION));
			assertEquals(streamId, json.get(WebSocketConstants.STREAM_ID));
			
		} catch (ParseException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
