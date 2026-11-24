package org.red5.server.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.red5.io.object.StreamAction;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.net.rtmp.status.StatusCodes;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.test.UnitTestBase;
import io.antmedia.websocket.WebSocketConstants;

@Tag("fast")
class StreamServicePublishSecurityTest extends UnitTestBase<StreamService> {

	private static final int RTMP_STREAM_ID = 1;

	private StreamService service;
	private IStreamCapableConnection connection;
	private IScope scope;
	private IClientBroadcastStream broadcast;
	private IProviderService provider;
	private IStreamPublishSecurity publishSecurity;
	private AppSettings appSettings;

	@BeforeEach
	void setUpPublisher() {
		service = spy(new StreamService());
		connection = mock(IStreamCapableConnection.class);
		scope = mock(IScope.class);
		broadcast = mock(IClientBroadcastStream.class);
		provider = mock(IProviderService.class);
		publishSecurity = mock(IStreamPublishSecurity.class);
		appSettings = new AppSettings();
		IContext context = mock(IContext.class);
		IStreamSecurityService securityService = mock(IStreamSecurityService.class);
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);

		when(connection.getConnectParams()).thenReturn(Map.of("path", "LiveApp"));
		when(connection.getScope()).thenReturn(scope);
		when(connection.getStreamId()).thenReturn(RTMP_STREAM_ID);
		when(connection.newBroadcastStream(RTMP_STREAM_ID)).thenReturn(broadcast);
		when(scope.getContext()).thenReturn(context);
		when(context.getBean(IProviderService.BEAN_NAME)).thenReturn(provider);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);
		when(application.getDataStore()).thenReturn(mock(DataStore.class));
		when(securityService.getStreamPublishSecurity()).thenReturn(Set.of(publishSecurity));
		when(publishSecurity.isPublishAllowed(any(IScope.class), anyString(), anyString(), any(),
				isNull(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(true);
		doReturn(securityService).when(service).getSecurityService(scope);
		doNothing().when(service).sendNSFailed(eq(connection), anyString(), anyString(),
				nullable(String.class), eq(RTMP_STREAM_ID));
		Red5.setConnectionLocal(connection);
	}

	@AfterEach
	void clearConnection() {
		Red5.setConnectionLocal(null);
	}

	@ParameterizedTest
	@MethodSource("unsafePublishes")
	void shouldRejectUnsafeNamesBeforeSecurityRegistrationOrFileOperations(String name, String mode) throws Exception {
		service.publish(name, mode);

		verify(service).sendNSFailed(eq(connection), eq(StatusCodes.NS_PUBLISH_BADNAME),
				anyString(), anyString(), eq(RTMP_STREAM_ID));
		verify(service, never()).verifySecurity(any(IScope.class), any(IStreamCapableConnection.class),
				anyString(), any(Number.class), any(), anyString(), any(StreamAction.class));
		verify(connection, never()).newBroadcastStream(any(Number.class));
		verifyNoInteractions(broadcast, provider, publishSecurity);
	}

	static Stream<Arguments> unsafePublishes() {
		return Stream.of(".", "..", "../victim",
				"..//..//..//..//..//..//..//tmp/ams_pwn?x=y",
				"..//..//OtherApp/streams/victim?x=y",
				"..//..//..//conf/red5.properties|x?x=y",
				"..//..//..//..//..//..//..//tmp/deltest/.*|x?x=y",
				"camera/../../conf/victim?x=y", "..%2F..%2Foutside?x=y",
				"camera\\..\\..\\conf\\victim?x=y")
				.flatMap(name -> publishModes().map(mode -> Arguments.of(name, mode)));
	}

	@ParameterizedTest
	@MethodSource("supportedPublishes")
	void shouldPreserveSupportedNamesCredentialsAndRecordingModes(PublishCase publish, String mode) throws Exception {
		when(connection.getConnectParams()).thenReturn(Map.of("path", publish.connectionPath()));
		appSettings.setPublishTokenControlEnabled(publish.pathTokenExpected());

		service.publish(publish.name(), mode);

		Map<String, String> parameters = publish.parameters();
		verify(publishSecurity).isPublishAllowed(scope, publish.streamId(), mode, parameters, null,
				parameters == null ? null : parameters.get(WebSocketConstants.TOKEN),
				parameters == null ? null : parameters.get(WebSocketConstants.SUBSCRIBER_ID),
				parameters == null ? null : parameters.get(WebSocketConstants.SUBSCRIBER_CODE));
		verify(provider).registerBroadcastStream(scope, publish.streamId(), broadcast);
		verify(broadcast).setPublishedName(publish.streamId());
		if (parameters != null) {
			verify(broadcast).setParameters(parameters);
		} else {
			verify(broadcast, never()).setParameters(any());
		}
		verify(broadcast).start();
		verify(broadcast).startPublishing();
		if (IClientStream.MODE_RECORD.equals(mode)) {
			verify(broadcast).saveAs(publish.streamId(), false);
		} else if (IClientStream.MODE_APPEND.equals(mode)) {
			verify(broadcast).saveAs(publish.streamId(), true);
		} else {
			verify(broadcast, never()).saveAs(anyString(), anyBoolean());
		}
	}

	static Stream<Arguments> supportedPublishes() {
		Map<String, String> pathParameters = Map.of(WebSocketConstants.STREAM_NAME, "camera.v1",
				WebSocketConstants.TOKEN, "token123", WebSocketConstants.SUBSCRIBER_ID, "alice",
				WebSocketConstants.SUBSCRIBER_CODE, "123456");
		Map<String, String> queryParameters = Map.of(WebSocketConstants.TOKEN, "token123",
				WebSocketConstants.SUBSCRIBER_ID, "alice", WebSocketConstants.SUBSCRIBER_CODE, "123456",
				"mainTrack", "main");
		List<PublishCase> publishes = List.of(
				new PublishCase("LiveApp", "123", false, "123", null),
				new PublishCase("LiveApp", "camera-1_main..v2", false, "camera-1_main..v2", null),
				new PublishCase("LiveApp", "camera.v1?token=token123&subscriberId=alice&subscriberCode=123456&mainTrack=main",
						false, "camera.v1", queryParameters),
				new PublishCase("LiveApp", "camera.v1/token123/alice/123456", false, "camera.v1", pathParameters),
				new PublishCase("LiveApp", "camera.v1%2Ftoken123%2Falice%2F123456", false, "camera.v1", pathParameters),
				new PublishCase("LiveApp/room", "camera.v1", false, "camera.v1", null),
				new PublishCase("LiveApp/camera.v1", "token123/alice/123456", true, "camera.v1", pathParameters));
		return publishes.stream().flatMap(publish -> publishModes().map(mode -> Arguments.of(publish, mode)));
	}

	@Test
	void shouldPreservePublishSecurityRejectionForValidNames() throws Exception {
		when(publishSecurity.isPublishAllowed(any(IScope.class), anyString(), anyString(), any(),
				isNull(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(false);

		service.publish("camera.v1?token=invalid", IClientStream.MODE_LIVE);

		verify(publishSecurity).isPublishAllowed(scope, "camera.v1", IClientStream.MODE_LIVE,
				Map.of("token", "invalid"), null, "invalid", null, null);
		verify(service).sendNSFailed(eq(connection), eq(StatusCodes.NS_PUBLISH_BADNAME),
				anyString(), eq("camera.v1"), eq(RTMP_STREAM_ID));
		verifyNoInteractions(broadcast, provider);
	}

	private static Stream<String> publishModes() {
		return Stream.of(IClientStream.MODE_LIVE, IClientStream.MODE_RECORD, IClientStream.MODE_APPEND);
	}

	record PublishCase(String connectionPath, String name, boolean pathTokenExpected, String streamId,
			Map<String, String> parameters) {
	}
}
