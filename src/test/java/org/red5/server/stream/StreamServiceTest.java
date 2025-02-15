package org.red5.server.stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.springframework.context.ApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.internal.util.MockUtil.resetMock;

public class StreamServiceTest {


	//Start writing test codes for StreamService because we're encountering problems here time to time. 

	@Test
	public void testPublishMetaData() {


		StreamService streamService = Mockito.spy(new StreamService());


		String streamId = "stream123";
		String param = "token=12345";
		String name = streamId + "?" + param;

		IStreamCapableConnection connection = Mockito.mock(IStreamCapableConnection.class);
		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(connection.getScope()).thenReturn(scope);

		Map<String, Object> mockMap = Mockito.mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return streamId;
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(connection).getConnectParams();

		IContext context = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(context);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getApplicationContext()).thenReturn(appContext);

		IStreamSecurityService securityService = Mockito.mock(IStreamSecurityService.class);

		Mockito.when(appContext.containsBean(IStreamSecurityService.BEAN_NAME)).thenReturn(true);
		Mockito.when(appContext.getBean(IStreamSecurityService.BEAN_NAME)).thenReturn(securityService);


		Set<IStreamPublishSecurity> publishSecuritySet = new HashSet<>();

		IStreamPublishSecurity publishSecurity = Mockito.mock(IStreamPublishSecurity.class);
		Mockito.when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any())).thenReturn(false);

		publishSecuritySet.add(publishSecurity);

		Mockito.when(securityService.getStreamPublishSecurity()).thenReturn(publishSecuritySet);


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

		IStreamCapableConnection connection = Mockito.mock(IStreamCapableConnection.class);
		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(connection.getScope()).thenReturn(scope);
		IClientBroadcastStream bs = Mockito.mock(IClientBroadcastStream.class);
		IProviderService providerService = Mockito.mock(IProviderService.class);

		Mockito.when(connection.newBroadcastStream(any())).thenReturn(bs);

		Map<String, Object> mockMap = Mockito.mock(Map.class);
		Object customObject = new Object() {
			@Override
			public String toString() {
				return "streamid";
			}
		};
		Mockito.doReturn(customObject).when(mockMap).get("path");
		Mockito.doReturn(mockMap).when(connection).getConnectParams();

		IContext context = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(context);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getApplicationContext()).thenReturn(appContext);

		IStreamSecurityService securityService = Mockito.mock(IStreamSecurityService.class);

		Mockito.when(appContext.containsBean(IStreamSecurityService.BEAN_NAME)).thenReturn(true);
		Mockito.when(appContext.getBean(IStreamSecurityService.BEAN_NAME)).thenReturn(securityService);
		Mockito.when(context.getBean(IProviderService.BEAN_NAME)).thenReturn(providerService);

		Set<IStreamPublishSecurity> publishSecuritySet = new HashSet<>();

		IStreamPublishSecurity publishSecurity = Mockito.mock(IStreamPublishSecurity.class);
		Mockito.when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any())).thenReturn(true);

		publishSecuritySet.add(publishSecurity);

		Mockito.when(publishSecurity.isPublishAllowed(any(), any(), any(), any(), any())).thenReturn(true);


		Mockito.when(securityService.getStreamPublishSecurity()).thenReturn(publishSecuritySet);


		Red5.setConnectionLocal(connection);

		streamService.publish(name);

		Mockito.verify(streamService, Mockito.times(1)).parsePathSegments(name);

		Mockito.verify(publishSecurity, Mockito.times(1)).isPublishAllowed(scope, streamId, IClientStream.MODE_LIVE, params, null );

		Mockito.verify(bs, Mockito.times(1)).startPublishing();

	}

	@Test
	public void testRtmpUrlFormat() {
		StreamService streamService = Mockito.spy(new StreamService());
		String streamId = "testStream";
		String token = "test_token";
		String subscriberId = "test_subscriber_id";
		String subscriberCode = "test_subscriber_code";

		String name = token+"/"+subscriberId+"/"+subscriberCode;

		IConnection conn = Mockito.mock(IConnection.class);
		Map<String, Object> mockMap = Mockito.mock(Map.class);
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

}
