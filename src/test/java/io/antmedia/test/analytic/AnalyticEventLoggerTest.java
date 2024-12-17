package io.antmedia.test.analytic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.analytic.AnalyticEventLogger;
import io.antmedia.analytic.model.AnalyticEvent;
import io.antmedia.analytic.model.PlayEvent;
import io.antmedia.analytic.model.WatchTimeEvent;
import io.antmedia.filter.JWTFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class AnalyticEventLoggerTest {
	
	@Test
	public void testLogPlay() {
		
		
		AnalyticEventLogger eventLogger = Mockito.spy(new AnalyticEventLogger());
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		
		AntMediaApplicationAdapter appAdaptor = new AntMediaApplicationAdapter();
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		
		appAdaptor.setScope(Mockito.mock(IScope.class));
		AppSettings appSettings = new AppSettings();
		appAdaptor.setAppSettings(appSettings);
		
		Mockito.when(eventLogger.getAppContext()).thenReturn(appContext);
		
		PlayEvent playEvent = new PlayEvent();
		playEvent.setClientIP("23.56.33.12");
		assertEquals("23.56.33.12", playEvent.getClientIP());

		playEvent.setSubscriberId("subscriberId");
		playEvent.setProtocol("hls");
		playEvent.setStreamId("streamId");
		playEvent.setTimeMs(136);
		assertEquals(136, playEvent.getTimeMs());

		
		playEvent.setEvent(PlayEvent.EVENT_PLAY_STARTED);
		assertNotEquals(0, playEvent.getTimeMs());
		playEvent.setTimeMs(0);
		assertEquals(0, playEvent.getTimeMs());

		assertNull(playEvent.getLogSource());
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Response response = eventLogger.postEvent(request, playEvent);
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertNotEquals(0, playEvent.getTimeMs());
		assertEquals(AnalyticEvent.LOG_SOURCE_CLIENT, playEvent.getLogSource());
		
		//because it gets client ip from the request and it's set null
		assertNull(playEvent.getClientIP());
		assertEquals("subscriberId", playEvent.getSubscriberId());
		assertEquals("hls", playEvent.getProtocol());
		assertEquals("streamId", playEvent.getStreamId());
		assertEquals(PlayEvent.EVENT_PLAY_STARTED, playEvent.getEvent());
		
		
		appSettings.setSecureAnalyticEndpoint(true);
		response = eventLogger.postEvent(request, playEvent);
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
		appSettings.setJwtSecretKey("testtest");
		
		String jwtToken = JWTFilter.generateJwtToken(appSettings.getJwtSecretKey(), System.currentTimeMillis() + 10000);
		playEvent.setToken(jwtToken);
		response = eventLogger.postEvent(request, playEvent);
		assertEquals(Status.OK.getStatusCode(), response.getStatus());


	}
	
	@Test
	public void testLogWatchTime() {
		
		AnalyticEventLogger eventLogger = Mockito.spy(new AnalyticEventLogger());
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		
		AntMediaApplicationAdapter appAdaptor = new AntMediaApplicationAdapter();
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		
		appAdaptor.setScope(Mockito.mock(IScope.class));
		AppSettings appSettings = new AppSettings();
		appAdaptor.setAppSettings(appSettings);
		
		Mockito.when(eventLogger.getAppContext()).thenReturn(appContext);
		
		WatchTimeEvent watchTimeEvent = new WatchTimeEvent();
		watchTimeEvent.setStartTimeMs(100);
		watchTimeEvent.setWatchTimeMs(500);
		assertNotEquals(0, watchTimeEvent.getTimeMs());
		watchTimeEvent.setTimeMs(0);
		assertEquals(0, watchTimeEvent.getTimeMs());

		assertNull(watchTimeEvent.getLogSource());
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Response response = eventLogger.postEvent(request, watchTimeEvent);
		assertEquals(200, response.getStatus());
		assertNotEquals(0, watchTimeEvent.getTimeMs());
		assertEquals(AnalyticEvent.LOG_SOURCE_CLIENT, watchTimeEvent.getLogSource());
		
		assertEquals(100, watchTimeEvent.getStartTimeMs());
		assertEquals(500, watchTimeEvent.getWatchTimeMs());
		
		appSettings.setSecureAnalyticEndpoint(true);
		response = eventLogger.postEvent(request, watchTimeEvent);
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
		appSettings.setJwtSecretKey("testtest");
		
		String jwtToken = JWTFilter.generateJwtToken(appSettings.getJwtSecretKey(), System.currentTimeMillis() + 10000);
		watchTimeEvent.setToken(jwtToken);
		response = eventLogger.postEvent(request, watchTimeEvent);
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
	}
	
	

}
