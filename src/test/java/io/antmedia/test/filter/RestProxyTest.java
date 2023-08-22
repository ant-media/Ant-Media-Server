package io.antmedia.test.filter;

import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.filter.RestProxyFilter;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.servlet.EndpointProxy;
import io.antmedia.security.ITokenService;
import io.antmedia.settings.ServerSettings;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RestProxyTest {
	protected static Logger logger = LoggerFactory.getLogger(IPFilterTest.class);
	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName() );
			e.printStackTrace();
		}
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		}
	};

	@Test
	public void testDoFilterPass() throws IOException, ServletException {
		RestProxyFilter restFilter = spy(new RestProxyFilter());

		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRemoteAddr("127.0.0.1");

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		ConfigurableWebApplicationContext webAppContext = mock(ConfigurableWebApplicationContext.class);
		Mockito.doReturn(webAppContext).when(restFilter).getAppContext();

		DataStoreFactory dtFactory = mock(DataStoreFactory.class);
		DataStore dtStore = mock(DataStore.class);

		Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
		Mockito.when(dtStore.isAvailable()).thenReturn(true);

		Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);

		AppSettings appSettings = new AppSettings();
		appSettings.setRemoteAllowedCIDR("127.0.0.1/8");

		appSettings.setIpFilterEnabled(true);
		Mockito.doReturn(appSettings).when(restFilter).getAppSettings();

		restFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
		assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());

		// Add App Settings tests
		MockHttpServletRequest httpServletRequest2 = new MockHttpServletRequest();
		httpServletRequest.setRemoteAddr("22.22.11.11");

		MockHttpServletResponse httpServletResponse2 = new MockHttpServletResponse();
		MockFilterChain filterChain2 = new MockFilterChain();

		appSettings.setRemoteAllowedCIDR("null");
		appSettings.setIpFilterEnabled(false);
		Mockito.doReturn(appSettings).when(restFilter).getAppSettings();

		restFilter.doFilter(httpServletRequest2, httpServletResponse2, filterChain2);
		assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());

	}

	@Test
	public void testGetStreamId() {
		RestProxyFilter restFilter = spy(new RestProxyFilter());

		String streamId = restFilter.getStreamId("rest/v2/broadcasts");
		assertNull(streamId);

		streamId = restFilter.getStreamId("rest/v2/broadcasts/");
		assertNull(streamId);

		streamId = restFilter.getStreamId("rest/v2/broadcasts/123456/");
		assertEquals("123456",streamId);

		streamId = restFilter.getStreamId("rest/v2/broadcasts/123456");
		assertEquals("123456",streamId);
	}

	@Test
	public void testDoFilterPassCluster() throws IOException, ServletException {
		RestProxyFilter restFilter = spy(new RestProxyFilter());

		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRemoteAddr("10.0.0.0");
		httpServletRequest.setRequestURI("/broadcasts/23456");

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		ConfigurableWebApplicationContext webAppContext = mock(ConfigurableWebApplicationContext.class);
		Mockito.doReturn(webAppContext).when(restFilter).getAppContext();

		DataStoreFactory dtFactory = mock(DataStoreFactory.class);
		DataStore dtStore = mock(DataStore.class);

		Broadcast broadcast = new Broadcast();
		broadcast.setOriginAdress("127.0.0.1");
		try{
			broadcast.setStreamId("stream1");

		}
		catch(Exception e ){
			logger.error("StreamId can't set");
			fail();
		}

		Mockito.doReturn(broadcast).when(dtStore).get(Mockito.anyString());

		Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
		Mockito.when(dtStore.isAvailable()).thenReturn(true);

		Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);
		Mockito.doReturn(true).when(webAppContext).containsBean(IClusterNotifier.BEAN_NAME);

		AppSettings appSettings = new AppSettings();
		appSettings.setRemoteAllowedCIDR("127.0.0.1/8");

		ServerSettings serverSettings = spy(new ServerSettings());
		Mockito.doReturn("172.0.0.0").when(serverSettings).getHostAddress();

		appSettings.setIpFilterEnabled(true);
		Mockito.doReturn(appSettings).when(restFilter).getAppSettings();
		Mockito.doReturn(serverSettings).when(restFilter).getServerSettings();

		httpServletRequest.setMethod(HttpMethod.POST);
		restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
		assertEquals(200, httpServletResponse.getStatus());

		broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		httpServletRequest.setMethod(HttpMethod.DELETE);
		filterChain = new MockFilterChain();
		httpServletRequest.setQueryString("query string");
		restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
		assertEquals(200, httpServletResponse.getStatus());

		httpServletRequest.setMethod(HttpMethod.PUT);
		filterChain = new MockFilterChain();
		httpServletRequest.addHeader("X-Forwarded-For", "170.0.0.0");

		restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
		assertEquals(200, httpServletResponse.getStatus());

		httpServletRequest.setMethod(HttpMethod.GET);
		filterChain = new MockFilterChain();
		httpServletRequest.setRequestURI("/v2/broadcasts/stream12314_239/rtmp-endpoint");
		restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
		assertEquals(200, httpServletResponse.getStatus());
		
		
		
		httpServletRequest.setMethod(HttpMethod.PUT);
		filterChain = new MockFilterChain();
		httpServletRequest.setRequestURI("/v2/br");
		restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
		assertEquals(200, httpServletResponse.getStatus());
		
	}

	@Test
	public void testEndpointProxy() {
		String jwtToken = "something";

		EndpointProxy endpointProxy = spy(new EndpointProxy(jwtToken));

		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		try {
			endpointProxy.initTarget("http://127.0.0.1");
			HttpResponse response = mock(HttpResponse.class);
			StatusLine statusLine = mock(StatusLine.class);
			Mockito.when(statusLine.getStatusCode()).thenReturn(304);
			Mockito.when(response.getStatusLine()).thenReturn(statusLine);
			Mockito.when(response.getAllHeaders()).thenReturn(new BasicHeader[]{ new BasicHeader("key", "value"), new BasicHeader("key", "value") } );

			Mockito.doReturn(response).when(endpointProxy).doExecute(Mockito.any(), Mockito.any(), Mockito.any());
			endpointProxy.service((HttpServletRequest)httpServletRequest, (HttpServletResponse)httpServletResponse);
			
			Mockito.when(statusLine.getStatusCode()).thenReturn(200);
			endpointProxy.service((HttpServletRequest)httpServletRequest, (HttpServletResponse)httpServletResponse);
			
			Mockito.doReturn(null).when(endpointProxy).doExecute(Mockito.any(), Mockito.any(), Mockito.any());
			endpointProxy.service((HttpServletRequest)httpServletRequest, (HttpServletResponse)httpServletResponse);
			
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testIsForwardedByAnotherNodeWithValidToken() {
		AppSettings appSettings = new AppSettings();
		appSettings.setClusterCommunicationKey("something");

		RestProxyFilter restProxyFilter = spy(new RestProxyFilter());
		ITokenService tokenService = mock(ITokenService.class);
		restProxyFilter.setTokenService(tokenService);
		doReturn(appSettings).when(restProxyFilter).getAppSettings();
		doReturn(tokenService).when(restProxyFilter).getTokenService();

		String jwtSecretKey = "sdfadfasf";
		String jwtInternalCommunicationToken = restProxyFilter.generateJwtToken(jwtSecretKey, System.currentTimeMillis()+10000);
		assertNotNull(jwtInternalCommunicationToken);

		when(tokenService.isJwtTokenValid(anyString(), anyString(), anyString(), Mockito.any()))
				.thenReturn(true);

		boolean actual = restProxyFilter.isNodeCommunicationTokenValid(jwtInternalCommunicationToken, jwtSecretKey, "reuest");

		assertEquals(true, actual);
	}

	@Test
	public void testIsForwardedByAnotherNodeWithNullToken() {
		// Arrange
		String jwtSecretKey = "yourStreamId";
		String jwtInternalCommunicationToken = null;


		RestProxyFilter restProxyFilter = new RestProxyFilter();
		ITokenService tokenService = mock(ITokenService.class); // Replace with the actual TokenService class
		restProxyFilter.setTokenService(tokenService);

		boolean actual = restProxyFilter.isNodeCommunicationTokenValid(jwtInternalCommunicationToken, jwtSecretKey, null);
		assertEquals(false, actual);
	}

	@Test
	public void testBlockSubscriberCluster() throws ServletException, IOException {
		RestProxyFilter restFilter = spy(new RestProxyFilter());

		MockHttpServletRequest httpServletRequest1 = new MockHttpServletRequest();
		String streamId = "testBlockSubscriber";
		String subscriberId = "subscriberTest";
		boolean playBlocked = true;
		long playBlockTime = System.currentTimeMillis();
		long playBlockedUntilTime = playBlockTime + 5000;
		String blockSubscriberData = "{\"subscriberId\":\"" + subscriberId + "\",\"playBlocked\":" + playBlocked + ",\"playBlockTime\":" + playBlockTime + ",\"playBlockedUntilTime\":" + playBlockedUntilTime + "}";

		Subscriber subscriber = new Subscriber();
		subscriber.setSubscriberId(subscriberId);
		subscriber.setStreamId(streamId);
		subscriber.setRegisteredNodeIp("1.1.1.1");
		httpServletRequest1.setRequestURI("broadcasts/"+streamId+"/subscribers/block");

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		MockFilterChain filterChain1 = new MockFilterChain();

		ConfigurableWebApplicationContext webAppContext = mock(ConfigurableWebApplicationContext.class);
		Mockito.doReturn(webAppContext).when(restFilter).getAppContext();
		Mockito.doReturn(webAppContext).when(restFilter).getWebApplicationContext();
		Mockito.doReturn(true).when(webAppContext).isRunning();

		DataStoreFactory dtFactory = mock(DataStoreFactory.class);
		DataStore dtStore = mock(DataStore.class);

		Broadcast broadcast = new Broadcast();
		try{
			broadcast.setStreamId(streamId);

		}
		catch(Exception e ){
			logger.error("StreamId can't set");
			fail();
		}

		Mockito.doReturn(broadcast).when(dtStore).get(Mockito.anyString());
		Mockito.doReturn(subscriber).when(dtStore).getSubscriber(streamId, subscriberId);

		Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
		Mockito.when(dtStore.isAvailable()).thenReturn(true);

		Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);
		Mockito.doReturn(true).when(webAppContext).containsBean(IClusterNotifier.BEAN_NAME);

		AppSettings appSettings = new AppSettings();
		ServerSettings serverSettings = spy(new ServerSettings());

		Mockito.doReturn(appSettings).when(restFilter).getAppSettings();
		Mockito.doReturn(serverSettings).when(restFilter).getServerSettings();

		httpServletRequest1.setMethod(HttpMethod.POST);
		httpServletRequest1.setContentType("application/json");
		httpServletRequest1.setContent(blockSubscriberData.getBytes());

		restFilter.doFilter(httpServletRequest1,httpServletResponse,filterChain1);
		assertEquals(301, httpServletResponse.getStatus());

		subscriber.setRegisteredNodeIp(ServerSettings.getGlobalHostAddress());

		MockHttpServletRequest httpServletRequest2 = new MockHttpServletRequest();

		httpServletRequest2.setRequestURI("broadcasts/"+streamId+"/subscribers/block");

		httpServletRequest2.setMethod(HttpMethod.POST);
		httpServletRequest2.setContentType("application/json");
		httpServletRequest2.setContent(blockSubscriberData.getBytes());

		Mockito.doReturn(subscriber).when(dtStore).getSubscriber(streamId, subscriberId);

		restFilter.doFilter(httpServletRequest2,httpServletResponse,filterChain1);
		assertEquals(301, httpServletResponse.getStatus());

		subscriber.setRegisteredNodeIp(null);

		MockHttpServletRequest httpServletRequest3 = new MockHttpServletRequest();

		httpServletRequest3.setRequestURI("broadcasts/"+streamId+"/subscribers/block");

		httpServletRequest3.setMethod(HttpMethod.POST);
		httpServletRequest3.setContentType("application/json");
		httpServletRequest3.setContent(blockSubscriberData.getBytes());
		MockFilterChain filterChain2 = new MockFilterChain();

		restFilter.doFilter(httpServletRequest3,httpServletResponse,filterChain2);
		assertEquals(301, httpServletResponse.getStatus());


	}
}
