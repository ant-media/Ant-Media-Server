package io.antmedia.test.websocket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.websocket.Session;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;


public class WebSocketCommunityHandlerTest {

    private WebSocketCommunityHandler webSocketCommunityHandler;
    private Session session;

    @Before
    public void setup() {
        session = Mockito.mock(Session.class);
        ApplicationContext context = mock(ApplicationContext.class);

		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		IScope scope = Mockito.mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		when(adaptor.getScope()).thenReturn(scope);
		when(context.getBean("web.handler")).thenReturn(adaptor);



		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(mock(AppSettings.class));
        webSocketCommunityHandler = Mockito.spy(new WebSocketCommunityHandler(context, session)); // Spy on the handler
    }

    @Test
    public void testSendSDPConfiguration() throws Exception {
        // Arrange
        String description = "sdpDescription";
        String type = "offer";
        String streamId = "stream1";
        String linkedSessionForSignaling = "linkedSession";
        String subscriberId = "subscriberId";

        JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, description);
		jsonResponseObject.put(WebSocketConstants.TYPE, type);
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonResponseObject.put(WebSocketConstants.LINK_SESSION, linkedSessionForSignaling);
		jsonResponseObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
		
        // Act
        webSocketCommunityHandler.sendSDPConfiguration(description, type, streamId, session, null, linkedSessionForSignaling, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponseObject), eq(session));
    }

    @Test
    public void testSendPublishStartedMessage() throws Exception {
        // Arrange
        String streamId = "stream1";
        String roomName = "room1";
        String subscriberId = "subscriberId";

        JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_STARTED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);

		if(roomName != null) {
			jsonObj.put(WebSocketConstants.ATTR_ROOM_NAME, roomName); //keep it for compatibility
			jsonObj.put(WebSocketConstants.ROOM, roomName);
		}
		jsonObj.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
        // Act
        webSocketCommunityHandler.sendPublishStartedMessage(streamId, session, roomName, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonObj), eq(session));
    }
    
    @Test
    public void testSendStreamingStartedMessage() throws Exception {
        // Arrange
        String streamId = "stream1";
        String roomName = "room1";
        String subscriberId = "subscriberId";

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
        expectedJson.put(WebSocketConstants.DEFINITION, WebSocketConstants.STREAMING_STARTED);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendStreamingStartedMessage(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(expectedJson), eq(session));
    }
    
    @Test
    public void testSendStreamIdInUse() throws Exception {
        // Arrange
        String streamId = "stream1";

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.STREAM_ID_IN_USE);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendStreamIdInUse(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }

    @Test
    public void testSendPongMessage() throws Exception {
        // Arrange
        JSONObject jsonResponseObject = new JSONObject();
        jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PONG_COMMAND);

        // Act
        webSocketCommunityHandler.sendPongMessage(session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponseObject), eq(session));
    }

    @Test
    public void testSendPublishFinishedMessage() throws Exception {
        // Arrange
        String streamId = "stream1";
        String subscriberId = "subscriberId";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
        jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_FINISHED);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

        // Act
        webSocketCommunityHandler.sendPublishFinishedMessage(streamId, session, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonObject), eq(session));
    }

    @Test
    public void testSendStartMessage() throws Exception {
        // Arrange
        String streamId = "stream1";
        String subscriberId = "subscriberId";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.START_COMMAND);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

        // Act
        webSocketCommunityHandler.sendStartMessage(streamId, session, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonObject), eq(session));
    }

    @Test
    public void testSendNoStreamIdSpecifiedError() throws Exception {
        // Arrange
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_ID_SPECIFIED);

        // Act
        webSocketCommunityHandler.sendNoStreamIdSpecifiedError(session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }
    
    
    @Test
    public void testSendRemoteDescriptionSetFailure() throws Exception {
        // Arrange
        String streamId = "stream1";
        String subscriberId = "subscriber1";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_REMOTE_DESCRIPTION);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

        // Act
        webSocketCommunityHandler.sendRemoteDescriptionSetFailure(session, streamId, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonObject), eq(session));
    }

    @Test
    public void testSendLocalDescriptionSetFailure() throws Exception {
        // Arrange
        String streamId = "stream1";
        String subscriberId = "subscriber1";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_LOCAL_DESCRIPTION);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

        // Act
        webSocketCommunityHandler.sendLocalDescriptionSetFailure(session, streamId, subscriberId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonObject), eq(session));
    }

    @Test
    public void testSendNotFoundJSON() throws Exception {
        // Arrange
        String streamId = "stream1";

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.ERROR_CODE, "404");
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_EXIST);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendNotFoundJSON(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
        
        
        webSocketCommunityHandler.sendNotFoundJSON(streamId, "reason", session);
        jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.ERROR_CODE, "404");
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_EXIST);
        jsonResponse.put(WebSocketConstants.INFORMATION , "reason");

        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
        
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }
    
    @Test
    public void testSendStreamStartingSoon() throws Exception {
        // Arrange
        String streamId = "stream1";

        JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.STREAMING_STARTS_SOON_DEFINITION);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendStreamingStartsSoonMessage(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
        
    }

    @Test
    public void testSendServerError() throws Exception {
        // Arrange
        String streamId = "stream1";

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.SERVER_ERROR_CHECK_LOGS);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendServerError(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }
   
   

    @Test
    public void testSendInvalidStreamNameError() throws Exception {
        // Arrange
        String streamId = "invalidStream";
        
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.INVALID_STREAM_NAME);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);

        // Act
        webSocketCommunityHandler.sendInvalidStreamNameError(streamId, session);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }

    @Test
    public void testSendRoomNotActiveInformation() throws Exception {
        // Arrange
        String roomId = "room1";

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.ROOM_NOT_ACTIVE);
        jsonResponse.put(WebSocketConstants.ROOM, roomId);

        // Act
        webSocketCommunityHandler.sendRoomNotActiveInformation(roomId);

        // Assert - Verify that sendMessage is called with the correct JSON object and session
        verify(webSocketCommunityHandler).sendMessage(eq(jsonResponse), eq(session));
    }

}
