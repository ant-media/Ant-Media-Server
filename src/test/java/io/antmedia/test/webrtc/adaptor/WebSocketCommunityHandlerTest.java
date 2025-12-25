package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.antmedia.webrtc.WebRTCUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.settings.ServerSettings;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

public class WebSocketCommunityHandlerTest {

	private WebSocketEndpoint wsHandlerReal;
	private WebSocketEndpoint wsHandler;
	private Session session;
	private Basic basicRemote;
	private HashMap userProperties;
	private static ApplicationContext appContext;
	private DataStore dataStore;

	public static final Logger logger = LoggerFactory.getLogger(WebSocketCommunityHandlerTest.class);

	public static class WebSocketEndpoint extends WebSocketCommunityHandler {
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
	public void before() 
	{
		appContext = Mockito.mock(ApplicationContext.class);
		when(appContext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		IScope scope = Mockito.mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		when(adaptor.getScope()).thenReturn(scope);

		when(adaptor.getServerSettings()).thenReturn(new ServerSettings());

		when(appContext.getBean("web.handler")).thenReturn(adaptor);

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
		broadcast.setUpdateTime(System.currentTimeMillis());
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		dataStore.save(broadcast);
		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler).sendStreamIdInUse(Mockito.anyString(), Mockito.any());


		//case status preparing
		streamId = "streamId" + (int)(Math.random()*10000);
		broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		broadcast.setUpdateTime(System.currentTimeMillis());
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		dataStore.save(broadcast);

		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(Mockito.anyString(), Mockito.any());


		// case no status

		RTMPAdaptor rtmpAdaptor = mock(RTMPAdaptor.class);


		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString(), Mockito.anyInt());


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


		verify(rtmpAdaptor).start();
		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(Mockito.anyString(), Mockito.any());



		// case no status
		streamId = "streamId" + (int)(Math.random()*10000);

		publishObject.put(WebSocketConstants.STREAM_ID, streamId);

		wsHandler.onMessage(session, publishObject.toJSONString());

		verify(rtmpAdaptor, Mockito.times(2)).start();
		verify(wsHandler, Mockito.times(2)).sendStreamIdInUse(Mockito.anyString(), Mockito.any());


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
	public void testPlayStream() {
		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);

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
	public void testPublishAndDisconnect() 
	{
		logger.info("testPublishAndDisconnect is running 0");
		String sessionId = String.valueOf((int)(Math.random()*10000));
		when(session.getId()).thenReturn(sessionId);

		String streamId = "streamId" + (int)(Math.random()*1000);

		logger.info("testPublishAndDisconnect is running 1");
		RTMPAdaptor rtmpAdaptor = Mockito.spy(wsHandler.getNewRTMPAdaptor("url", 360));
		doNothing().when(rtmpAdaptor).start();
		doNothing().when(rtmpAdaptor).stop();

		doReturn(rtmpAdaptor).when(wsHandler).getNewRTMPAdaptor(Mockito.anyString(), Mockito.anyInt());


		logger.info("testPublishAndDisconnect is running 2");
		JSONObject publishObject = new JSONObject();
		publishObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject.put(WebSocketConstants.STREAM_ID, streamId);
		wsHandler.onMessage(session, publishObject.toJSONString());

		logger.info("testPublishAndDisconnect is running 3");
		verify(rtmpAdaptor).setSession(session);
		verify(rtmpAdaptor).setStreamId(streamId);
		verify(rtmpAdaptor).start();

		logger.info("testPublishAndDisconnect is running 4");
		wsHandler.onClose(session);

		logger.info("testPublishAndDisconnect is running 5");
		verify(rtmpAdaptor).stop();
		logger.info("testPublishAndDisconnect is running 6");

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
			verify(rtmpAdaptor, times(1)).addIceCandidate(argument.capture());
			IceCandidate icecandidate = argument.getValue();
			assertEquals(sdp, icecandidate.sdp);
			assertEquals(type,icecandidate.sdpMid);
			assertEquals(label,icecandidate.sdpMLineIndex);
		}

		{

			JSONObject takeCandidate = new JSONObject();
			takeCandidate.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);

			//don't send canidate id
			//String type = ""  +(int)(Math.random() * 91000);
			//takeCandidate.put(WebSocketConstants.CANDIDATE_ID, type );
			String sdp = ""  +(int)(Math.random() * 91000);;
			takeCandidate.put(WebSocketConstants.CANDIDATE_SDP, sdp);
			int label = (int)(Math.random() * 91000);;
			takeCandidate.put(WebSocketConstants.CANDIDATE_LABEL, label);
			takeCandidate.put(WebSocketConstants.STREAM_ID, streamId);

			wsHandler.onMessage(session, takeCandidate.toJSONString());

			ArgumentCaptor<IceCandidate> argument = ArgumentCaptor.forClass(IceCandidate.class);
			verify(rtmpAdaptor, times(2)).addIceCandidate(argument.capture());
			IceCandidate icecandidate = argument.getValue();
			assertEquals(sdp, icecandidate.sdp);
			assertEquals("0",icecandidate.sdpMid);
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

		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());

		String streamId2 = "streamId_" + (int)(Math.random()*1000);
		JSONObject publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());

		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());
		
		streamId2 = "stream.Id" + (int)(Math.random()*1000);
		publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());

		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());
		
		streamId2 = "streamId.fsdfsf324" + (int)(Math.random()*1000);
		publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());

		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());
		
		streamId2 = "streamId-fsdfs_f3.24" + (int)(Math.random()*1000);
		publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());

		verify(wsHandler, Mockito.never()).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());

		streamId2 = "streamId_:?" + (int)(Math.random()*1000);
		publishObject2 = new JSONObject();
		publishObject2.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
		publishObject2.put(WebSocketConstants.STREAM_ID, streamId2);
		wsHandler.onMessage(session, publishObject2.toJSONString());

		verify(wsHandler, Mockito.timeout(1000)).sendInvalidStreamNameError(Mockito.anyString(), Mockito.any());
		
		
		
		
		
		
	}

	@Test
	public void testWebSocketConstants() {
		assertEquals("already_playing", WebSocketConstants.ALREADY_PLAYING);
		assertEquals("targetBitrate", WebSocketConstants.TARGET_BITRATE);
		assertEquals("bitrateMeasurement", WebSocketConstants.BITRATE_MEASUREMENT);
	}

	@Test
	public void testThrowExceptionInSendMessage() {
		wsHandler.setSession(session);

		try {
			Mockito.doThrow(new IOException("exception")).when(basicRemote).sendText(Mockito.anyString());
			JSONObject json = new JSONObject();
			json.put(WebSocketConstants.COMMAND, WebSocketConstants.PING_COMMAND);
			wsHandler.sendMessage(json, session);


			Mockito.doThrow(new IOException()).when(basicRemote).sendText(Mockito.anyString());
			wsHandler.sendMessage(json, session);

			Mockito.doThrow(new IOException("WebSocket session has been closed")).when(basicRemote).sendText(Mockito.anyString());
			wsHandler.sendMessage(json, session);		
		} 
		catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSendRoomInformation() 
	{
		String roomId = "roomId12345";

		HashMap<String,String> streamDetailsMap = new HashMap<>();
		streamDetailsMap.put("streamId1",null);
		streamDetailsMap.put("streamId2","streamName2");
		streamDetailsMap.put("streamId3",null);
		streamDetailsMap.put("streamId4","streamName4");
		wsHandler.setSession(session);
		wsHandler.sendRoomInformation(streamDetailsMap, roomId);


		ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));

		JSONArray jsonStreamIdArray = new JSONArray();
		JSONArray jsonStreamNameArray = new JSONArray();

		for (HashMap.Entry<String, String> e : streamDetailsMap.entrySet()) {
			jsonStreamIdArray.add(e.getKey());
			JSONObject jsStreamObject = new JSONObject();
			jsStreamObject.put(WebSocketConstants.STREAM_ID, e.getKey());
			jsStreamObject.put(WebSocketConstants.STREAM_NAME, e.getValue());
			jsStreamObject.put(WebSocketConstants.META_DATA, null);
			jsonStreamNameArray.add(jsStreamObject);
		}

		JSONObject json = argument.getValue();
		assertEquals(WebSocketConstants.ROOM_INFORMATION_NOTIFICATION, json.get(WebSocketConstants.COMMAND));	
		assertEquals(roomId, json.get(WebSocketConstants.ROOM));
		assertEquals(jsonStreamIdArray, json.get(WebSocketConstants.STREAMS_IN_ROOM));
		assertEquals(jsonStreamNameArray, json.get(WebSocketConstants.STREAM_LIST_IN_ROOM));


	}

	@Test
	public void testSendJoinedRoomInformation() {
		String roomId = "roomId12345";
		String streamId = "stream34567";

		HashMap<String,String> streamDetailsMap = new HashMap<>();
		streamDetailsMap.put("streamId1",null);
		streamDetailsMap.put("streamId2","streamName2");
		streamDetailsMap.put("streamId3",null);
		streamDetailsMap.put("streamId4","streamName4");

		wsHandler.setSession(session);

		wsHandler.sendJoinedRoomMessage(roomId, streamId, streamDetailsMap, new HashMap<>());


		ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));

		JSONArray jsonStreamIdArray = new JSONArray();
		JSONArray jsonStreamNameArray = new JSONArray();

		for (HashMap.Entry<String, String> e : streamDetailsMap.entrySet()) {
			jsonStreamIdArray.add(e.getKey());
			JSONObject jsStreamObject = new JSONObject();
			jsStreamObject.put(WebSocketConstants.STREAM_ID, e.getKey());
			jsStreamObject.put(WebSocketConstants.STREAM_NAME, e.getValue());
			jsStreamObject.put(WebSocketConstants.META_DATA, null);
			jsonStreamNameArray.add(jsStreamObject);
		}

		JSONObject json = argument.getValue();
		assertEquals(WebSocketConstants.NOTIFICATION_COMMAND, json.get(WebSocketConstants.COMMAND));	
		assertEquals(roomId, json.get(WebSocketConstants.ROOM));
		assertEquals(jsonStreamIdArray, json.get(WebSocketConstants.STREAMS_IN_ROOM));
		assertEquals(jsonStreamNameArray, json.get(WebSocketConstants.STREAM_LIST_IN_ROOM));
		assertEquals(WebSocketConstants.JOINED_THE_ROOM, json.get(WebSocketConstants.DEFINITION));
		assertEquals(streamId, json.get(WebSocketConstants.STREAM_ID));


	}

	@Test
	public void testSendPublishStarted() {
		String roomId = "roomId12345";
		String streamId = "stream34567";
		JSONArray jsonStreamArray = new JSONArray();

		wsHandler.setSession(session);

		wsHandler.sendPublishStartedMessage(streamId,  session, roomId, ""); 

		ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
		verify(wsHandler).sendMessage(argument.capture(), Mockito.eq(session));

		JSONObject json = argument.getValue();
		assertEquals(WebSocketConstants.NOTIFICATION_COMMAND, json.get(WebSocketConstants.COMMAND));	
		assertEquals(roomId, json.get(WebSocketConstants.ROOM));
		assertEquals(WebSocketConstants.PUBLISH_STARTED, json.get(WebSocketConstants.DEFINITION));
		assertEquals(streamId, json.get(WebSocketConstants.STREAM_ID));


	}

	@Test
	public void testUserAgent() {
		assertEquals("N/A", wsHandler.getUserAgent());

		String userAgent = "dummy agent";
		wsHandler.setUserAgent(userAgent);
		assertEquals(userAgent, wsHandler.getUserAgent());
	}
	
	@Test
	public void testClientIP() {
		assertEquals("N/A", wsHandler.getClientIP());

		String clienTIP = "a.b.c.d";
		wsHandler.setClientIP(clienTIP);
		assertEquals(clienTIP, wsHandler.getClientIP());
	}

	@Test
	public void testGetSDP() {
		String description = "dummyDescripton";
		String type = "dummyType";
		String streamId = "dummyStreamId";

		int trackSize = RandomUtils.nextInt(0,5)+1;
		Map<String, String> midSidMap = new HashMap<>();
		for (int i = 0; i < trackSize; i++) {
			midSidMap.put("mid"+i, "sid"+i);
		}
		JSONObject json = WebSocketCommunityHandler.getSDPConfigurationJSON(description, type, streamId, midSidMap, null, "");

		assertEquals(WebSocketConstants.TAKE_CONFIGURATION_COMMAND, json.get(WebSocketConstants.COMMAND));
		assertEquals(description, json.get(WebSocketConstants.SDP));
		assertEquals(type, json.get(WebSocketConstants.TYPE));
		assertEquals(streamId, json.get(WebSocketConstants.STREAM_ID));

		JSONObject jsonMap = (JSONObject) json.get(WebSocketConstants.ID_MAPPING);
		for (int i = 0; i < trackSize; i++) {
			assertEquals("sid"+i, jsonMap.get("mid"+i));
		}
	}
    @Test
    public void testSdpMediaTypeValid(){
        String sdp = "v=0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 \n"
                + "a=rtpmap:111 opus/48000/2\n";

        assertTrue(WebRTCUtils.validateSdpMediaPayloads(sdp));

        sdp = "v=0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110\n"
                + "a=rtpmap:111 opus/4800/2\n" //opus should not be 48000
                + "a=rtpmap:107 rtx/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));

        sdp = "v=0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110\n"
                + "a=rtpmap:111 opus/48000/1\n" //opus should not be 1
                + "a=rtpmap:107 rtx/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));

        // valid all payload type available
        sdp = "v=0\n"
                + "o=- 7847717452155503175 2 IN IP4 127.0.0.1\n"
                + "s=-\n"
                + "t=0 0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110\n"
                + "a=rtpmap:111 opus/48000/2\n"
                + "a=rtpmap:63 red/48000/2\n"
                + "a=rtpmap:110 telephone-event/48000\n"
                + "m=video 9 UDP/TLS/RTP/SAVPF 106 107\n"
                + "a=rtpmap:106 H264/90000\n"
                + "a=rtpmap:107 rtx/90000\n";

        assertTrue(WebRTCUtils.validateSdpMediaPayloads(sdp));

        //missing payload type
        sdp = "v=0\n"
                + "o=- 7847717452155503175 2 IN IP4 127.0.0.1\n"
                + "s=-\n"
                + "t=0 0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63\n"
                + "a=rtpmap:111 opus/48000/2\n"
                + "a=rtpmap:110 telephone-event/48000\n"
                + "m=video 9 UDP/TLS/RTP/SAVPF 106 107\n"
                + "a=rtpmap:106 H264/90000\n"
                + "a=rtpmap:107 rtx/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));

        // extra payload type for audio
        sdp = "v=0\n"
                + "o=- 7847717452155503175 2 IN IP4 127.0.0.1\n"
                + "s=-\n"
                + "t=0 0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110 10\n"
                + "a=rtpmap:111 opus/48000/2\n"
                + "a=rtpmap:63 red/48000/2\n"
                + "a=rtpmap:110 telephone-event/48000\n"
                + "m=video 9 UDP/TLS/RTP/SAVPF 106 107\n"
                + "a=rtpmap:106 H264/90000\n"
                + "a=rtpmap:107 rtx/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));

        //extra payload type for video
        sdp = "v=0\n"
                + "o=- 7847717452155503175 2 IN IP4 127.0.0.1\n"
                + "s=-\n"
                + "t=0 0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110\n"
                + "a=rtpmap:111 opus/48000/2\n"
                + "a=rtpmap:63 red/48000/2\n"
                + "a=rtpmap:110 telephone-event/48000\n"
                + "m=video 9 UDP/TLS/RTP/SAVPF 106 107 10\n"
                + "a=rtpmap:106 H264/90000\n"
                + "a=rtpmap:107 rtx/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));

        //missing payload type for video
        sdp = "v=0\n"
                + "o=- 7847717452155503175 2 IN IP4 127.0.0.1\n"
                + "s=-\n"
                + "t=0 0\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111 63 110\n"
                + "a=rtpmap:111 opus/48000/2\n"
                + "a=rtpmap:63 red/48000/2\n"
                + "a=rtpmap:110 telephone-event/48000\n"
                + "m=video 9 UDP/TLS/RTP/SAVPF 106 107\n"
                + "a=rtpmap:106 H264/90000\n";

        assertFalse(WebRTCUtils.validateSdpMediaPayloads(sdp));
    }

}
