package org.red5.server.stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.red5.io.object.StreamAction;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.stream.*;
import org.red5.server.scope.BasicScope;
import org.red5.server.scope.Scope;
import org.red5.server.util.ScopeUtils;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.websocket.WebSocketConstants;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.MockUtil.resetMock;

public class StreamServiceTest {


	//Start writing test codes for StreamService because we're encountering problems here time to time. 

	@Test
	public void testPublishMetaData() {


		StreamService streamService = Mockito.spy(new StreamService());


		String streamId = "stream123";
		String param = "token=12345";
		String name = streamId + "?" + param;

		IStreamCapableConnection connection = mock(IStreamCapableConnection.class);
		IScope scope = mock(IScope.class);
		when(connection.getScope()).thenReturn(scope);

		Map<String, Object> mockMap = mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return streamId;
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(connection).getConnectParams();

		IContext context = mock(IContext.class);
		when(scope.getContext()).thenReturn(context);

		ApplicationContext appContext = mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);

		IStreamSecurityService securityService = mock(IStreamSecurityService.class);

		when(appContext.containsBean(IStreamSecurityService.BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IStreamSecurityService.BEAN_NAME)).thenReturn(securityService);


		Set<IStreamPublishSecurity> publishSecuritySet = new HashSet<>();

		IStreamPublishSecurity publishSecurity = mock(IStreamPublishSecurity.class);
		when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(false);

		publishSecuritySet.add(publishSecurity);

		when(securityService.getStreamPublishSecurity()).thenReturn(publishSecuritySet);


		Red5.setConnectionLocal(connection);

		Mockito.doNothing().when(streamService).sendNSFailed(any(), any(), any(), any(), any());

		streamService.publish(name, null);


		Mockito.verify(streamService).sendNSFailed(any(), any(), any(), any(), any());

	}

	@Test
	public void testPublishUrlSegmentParams() {
		StreamService streamService = Mockito.spy(new StreamService());
		String streamId = "testStream";
		String token = "test_token";
		String subscriberId = "test_subscriber_id";
		String subscriberCode = "test_subscriber_code";

		String name = streamId+"/"+token+"/"+subscriberId+"/"+subscriberCode;

		Map<String, String> params = streamService.parsePathSegments(name);
		resetMock(streamService);

		IStreamCapableConnection connection = mock(IStreamCapableConnection.class);
		IScope scope = mock(IScope.class);
		when(connection.getScope()).thenReturn(scope);
		IClientBroadcastStream bs = mock(IClientBroadcastStream.class);
		IProviderService providerService = mock(IProviderService.class);

		when(connection.newBroadcastStream(any())).thenReturn(bs);

		Map<String, Object> mockMap = mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return "streamid";
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(connection).getConnectParams();

		IContext context = mock(IContext.class);
		when(scope.getContext()).thenReturn(context);

		ApplicationContext appContext = mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);

		IStreamSecurityService securityService = mock(IStreamSecurityService.class);

		when(appContext.containsBean(IStreamSecurityService.BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IStreamSecurityService.BEAN_NAME)).thenReturn(securityService);
		when(context.getBean(IProviderService.BEAN_NAME)).thenReturn(providerService);
		
		AntMediaApplicationAdapter antMediaApplicationAdapter = mock(AntMediaApplicationAdapter.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(antMediaApplicationAdapter);
		
		when(antMediaApplicationAdapter.getDataStore()).thenReturn(mock(DataStore.class));

		Set<IStreamPublishSecurity> publishSecuritySet = new HashSet<>();

		IStreamPublishSecurity publishSecurity = mock(IStreamPublishSecurity.class);
		when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

		publishSecuritySet.add(publishSecurity);

		when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(true);


		when(securityService.getStreamPublishSecurity()).thenReturn(publishSecuritySet);


		Red5.setConnectionLocal(connection);

		streamService.publish(name);

		Mockito.verify(streamService, Mockito.times(1)).parsePathSegments(name);

		Mockito.verify(publishSecurity, Mockito.times(1)).isPublishAllowed(scope, streamId, IClientStream.MODE_LIVE, params, null, token, subscriberId, subscriberCode);

		Mockito.verify(bs, Mockito.times(1)).startPublishing();

	}
	
	@Test
	public void testVerifySecurityRTMPPlayback() {
		StreamService streamService = Mockito.spy(new StreamService());
		
		IScope scope = Mockito.mock(Scope.class);
		IStreamCapableConnection streamConn = Mockito.mock(IStreamCapableConnection.class);
		String streamIdText = "stream123";
				
		Number streamId = 123132;
		String subscriberId = "subId";

		Map<String, String> params = new HashMap<String, String>();
		params.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

		String mode = "mode";
		StreamAction action = StreamAction.PLAY;
		
		when(streamConn.getScope()).thenReturn(scope);
		when(streamConn.getStreamId()).thenReturn(streamId);
		
		Mockito.doReturn(Mockito.mock(IStreamSecurityService.class)).when(streamService).getSecurityService(scope);
		
        AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
        IContext context = Mockito.mock(IContext.class);
        when(scope.getContext()).thenReturn(context);
		when(scope.getContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(adaptor);
		
		DataStore dataStore = new InMemoryDataStore("junit");
		when(adaptor.getDataStore()).thenReturn(dataStore);
		dataStore.blockSubscriber(streamIdText, subscriberId, Subscriber.PUBLISH_AND_PLAY_TYPE, 10000);
		
		Mockito.doNothing().when(streamService).sendNSFailed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
		boolean result = streamService.verifySecurity(scope, streamConn, streamIdText, streamId, params, mode, action);
		assertFalse(result);
		
		Mockito.verify(streamService, Mockito.times(1)).sendNSFailed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
		
	}

	@Test
	public void testRtmpUrlFormat() {
		StreamService streamService = Mockito.spy(new StreamService());
		String streamId = "testStream";
		String token = "test_token";
		String subscriberId = "test_subscriber_id";
		String subscriberCode = "test_subscriber_code";

		String name = token+"/"+subscriberId+"/"+subscriberCode;

		IConnection conn = mock(IConnection.class);
		Map<String, Object> mockMap = mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return "LiveApp/"+streamId;
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(conn).getConnectParams();
		Red5.setConnectionLocal(conn);

		streamService.publish(name,"live");
		Mockito.verify(streamService).parsePathSegments(streamId + "/" +name);

	}

	@Test
	public void testStreamPublishSecurityServiceHandlers() {
		StreamService streamService = Mockito.spy(new StreamService());
		String streamId = "testStream";
		String token = "test_token";
		String subscriberId = "test_subscriber_id";
		String subscriberCode = "test_subscriber_code";

		String name = token+"/"+subscriberId+"/"+subscriberCode;

		IStreamCapableConnection conn = mock(IStreamCapableConnection.class);
		Map<String, Object> mockMap = mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return "LiveApp/"+streamId;
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(conn).getConnectParams();
		Red5.setConnectionLocal(conn);

		IStreamSecurityService mockSecurityService = mock(IStreamSecurityService.class);
		IStreamPublishSecurity mockHandler = mock(IStreamPublishSecurity.class);

		Set<IStreamPublishSecurity> mockHandlers = new HashSet<>();
		mockHandlers.add(mockHandler);

		when(mockSecurityService.getStreamPublishSecurity()).thenReturn(mockHandlers);

		try (MockedStatic<ScopeUtils> mockedStatic = mockStatic(ScopeUtils.class)) {
			mockedStatic.when(() -> ScopeUtils.getScopeService(any(), any()))
					.thenReturn(mockSecurityService);

			doReturn(false).when(mockHandler).isPublishAllowed(any(), any(), any(),any() , any(), anyString(), anyString(), anyString());
			doNothing().when(streamService).sendNSFailed(any(),anyString(),anyString(),anyString(),any());

			Map<String, String> params = new HashMap<>();
			params.put("subscriberId", "test_subscriber_id");
			params.put("subscriberCode", "test_subscriber_code");
			params.put("streamName", "testStream");
			params.put("token", "test_token");

			streamService.publish(name,"live");

			verify(mockHandler).isPublishAllowed(conn.getScope(), streamId, "live",params , null, token, subscriberId, subscriberCode);

		}
	}
	@Test
	public void testStreamPlaySecurityServiceHandlers(){
		StreamService streamService = Mockito.spy(new StreamService());
		Scope scope = Mockito.mock(Scope.class);
		String streamId = "testStream";
		String token = "test_token";
		String subscriberId = "test_subscriber_id";
		String subscriberCode = "test_subscriber_code";

		String name = streamId +"/"+token+"/"+subscriberId+"/"+subscriberCode;

		IStreamCapableConnection conn = mock(IStreamCapableConnection.class);
		Map<String, Object> mockMap = mock(Map.class);
		doReturn(scope).when(conn).getScope();
		Object customObject = new Object() {
			@Override
			public String toString() {
				return "LiveApp/"+streamId;
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(conn).getConnectParams();
		Mockito.doReturn(1).when(conn).getStreamId();
		Red5.setConnectionLocal(conn);
		
		when(scope.getContext()).thenReturn(mock(IContext.class));
		AntMediaApplicationAdapter adaptor = mock(AntMediaApplicationAdapter.class);
		when(scope.getContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(adaptor);
		when(adaptor.getDataStore()).thenReturn(new InMemoryDataStore("junit"));

		IStreamSecurityService mockSecurityService = mock(IStreamSecurityService.class);
		IStreamPlaybackSecurity mockHandler = mock(IStreamPlaybackSecurity.class);

		Set<IStreamPlaybackSecurity> mockHandlers = new HashSet<>();
		mockHandlers.add(mockHandler);

		when(mockSecurityService.getStreamPlaybackSecurity()).thenReturn(mockHandlers);

		try (MockedStatic<ScopeUtils> mockedStatic = mockStatic(ScopeUtils.class)) {
			mockedStatic.when(() -> ScopeUtils.getScopeService(any(), any()))
					.thenReturn(mockSecurityService);

			doReturn(false).when(mockHandler).isPlayAllowed(any(), any(), any(),any() , any(), anyString(), anyString(), anyString());
			doNothing().when(streamService).sendNSFailed(any(),anyString(),anyString(),anyString(),any());

			Map<String, String> params = new HashMap<>();
			params.put("subscriberId", "test_subscriber_id");
			params.put("subscriberCode", "test_subscriber_code");
			params.put("streamName", "testStream");
			params.put("token", "test_token");

			streamService.play(name,133);

			verify(mockHandler).isPlayAllowed(scope, streamId, "",params , null, token, subscriberId, subscriberCode);

		}
	}
}
